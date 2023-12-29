package nl.juriantech.questapi.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import nl.juriantech.questapi.QuestAPI;
import nl.juriantech.questapi.interfaces.DatabaseInterface;
import nl.juriantech.questapi.objects.Quest;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseImplementationMongoDB implements DatabaseInterface {
    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public boolean connect(String connectionString, String databaseName) {
        try {
            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(databaseName);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public CompletableFuture<Void> saveData(String collection, String key, Object data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = new Document("_id", key)
                        .append("data", data);
                database.getCollection(collection).insertOne(document);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Object> getData(String collection, String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("_id", key);
                Document result = database.getCollection(collection).find(query).first();

                return result;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private CompletableFuture<List<Document>> getAllDataFrom(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                List<Document> dataList = new ArrayList<>();

                for (Document document : collection.find()) {
                    dataList.add(document);
                }

                return dataList;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<Void> updateData(String collection, String key, Object newData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> mongoCollection = database.getCollection(collection);
                Document query = new Document("_id", key);

                FindIterable<Document> findIterable = mongoCollection.find(query);
                Document existingDocument = findIterable.first();

                if (existingDocument != null) {
                    Document update = new Document("$set", new Document("data", newData));
                    mongoCollection.updateOne(query, update);
                } else {
                    saveData(collection, key, newData).join();
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteData(String collection, String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("_id", key);
                database.getCollection(collection).deleteOne(query);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> saveQuestToDatabase(Quest quest) {
        Map<String, Object> questData = new HashMap<>();
        questData.put("questId", quest.getQuestId());
        questData.put("maxLevels", quest.getMaxLevels());
        questData.put("databaseUpdateIntervalSeconds", quest.getDatabaseUpdateIntervalSeconds());

        return saveData("all_quests", quest.getQuestId(), questData);
    }

    @Override
    public CompletableFuture<Map<String, Quest>> loadAllQuestsToHashMap(QuestAPI plugin) {
        CompletableFuture<Map<String, Quest>> future = new CompletableFuture<>();

        getAllDataFrom("all_quests").thenApplyAsync(allQuestsData -> {
            Map<String, Quest> quests = new HashMap<>();

            if (allQuestsData != null) {
                for (Document questData : allQuestsData) {
                    String questId = questData.getString("questId");
                    int maxLevels = questData.getInteger("maxLevels");
                    int databaseUpdateIntervalSeconds = questData.getInteger("databaseUpdateIntervalSeconds");

                    Quest quest = new Quest(plugin, questId, maxLevels, this, databaseUpdateIntervalSeconds);
                    quests.put(questId, quest);
                }

                future.complete(quests);
            } else {
                future.completeExceptionally(new IllegalArgumentException("Invalid format for quests data"));
            }

            return quests;
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> levelUp(UUID playerUUID, String questId, int newLevel) {
        return updateData("player_quest_data", getKey(questId, playerUUID), newLevel);
    }

    @Override
    public CompletableFuture<Map<UUID, Integer>> loadPlayerProgress(String questId, QuestAPI plugin) {
        Map<UUID, Integer> playerProgress = new HashMap<>();
        CompletableFuture<Map<UUID, Integer>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                CompletableFuture<Object> progressFuture = getData("player_quest_data", getKey(questId, player.getUniqueId()));
                progressFuture.thenAcceptAsync(data -> {
                    if (data != null && data instanceof HashMap) {
                        HashMap<?, ?> progressData = (HashMap<?, ?>) data;
                        Object progressValue = progressData.get("data");

                        if (progressValue instanceof Integer) {
                            int intValue = (Integer) progressValue;
                            playerProgress.put(player.getUniqueId(), intValue);
                        }
                    }
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
            }
            future.complete(playerProgress);
        });

        return future;
    }

    public String getKey(String questId, UUID playerUUID) {
        return questId + "_" + playerUUID;
    }
}