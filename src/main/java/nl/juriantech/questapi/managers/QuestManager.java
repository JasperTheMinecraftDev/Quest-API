package nl.juriantech.questapi.managers;

import nl.juriantech.questapi.interfaces.DatabaseInterface;
import nl.juriantech.questapi.objects.Quest;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuestManager {
    private final DatabaseInterface database;
    private final Map<String, Quest> quests;

    public QuestManager(DatabaseInterface database) {
        this.database = database;
        this.quests = new HashMap<>();
    }

    public void addQuest(Quest quest) {
        quests.put(quest.getQuestId(), quest);
        saveQuestToDatabase(quest);
    }

    public Quest getQuest(String questId) {
        return quests.get(questId);
    }

    public CompletableFuture<Void> saveAllQuestsToDatabase() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        for (Quest quest : quests.values()) {
            saveQuestToDatabase(quest)
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                });
        }

        future.complete(null);
        return future;
    }

    private CompletableFuture<Void> saveQuestToDatabase(Quest quest) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Convert the Quest object to a structure that's suitable for a MongoDB document
        Map<String, Object> questDocument = new HashMap<>();
        questDocument.put("questId", quest.getQuestId());
        questDocument.put("maxLevels", quest.getMaxLevels());
        questDocument.put("databaseUpdateIntervalSeconds", quest.getDatabaseUpdateIntervalSeconds());

        CompletableFuture<Void> saveQuestInfoFuture = database.saveData("all_quests", quest.getQuestId(), questDocument);
        saveQuestInfoFuture.thenAccept(result -> future.complete(null))
            .exceptionally(e -> {
                future.completeExceptionally(e);
                return null;
            });

        return future;
    }

    public CompletableFuture<Void> loadAllQuestsToHashMap() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture<List<Document>> allQuestsDataFuture = database.getAllDataFrom("all_quests");
        allQuestsDataFuture.thenAccept(allQuestsData -> {
            if (allQuestsData != null) {
                quests.clear();

                for (Document questDocument : allQuestsData) {
                    Object data = questDocument.get("data");

                    if (data instanceof Document questData) {
                        String questId = questData.getString("questId");
                        int maxLevels = questData.getInteger("maxLevels");
                        int databaseUpdateIntervalSeconds = questData.getInteger("databaseUpdateIntervalSeconds");

                        Quest quest = new Quest(questId, maxLevels, database, databaseUpdateIntervalSeconds);
                        quests.put(questId, quest);
                    }
                }

                future.complete(null);
            } else {
                future.completeExceptionally(new IllegalArgumentException("Invalid format for quests data"));
            }
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }


    public Map<String, Quest> getAllQuests() {
        return quests;
    }
}