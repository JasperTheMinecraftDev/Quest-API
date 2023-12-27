package nl.juriantech.questapi.managers;

import nl.juriantech.questapi.interfaces.DatabaseInterface;
import nl.juriantech.questapi.objects.Quest;

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

        Map<String, Object> questData = new HashMap<>();
        questData.put("questId", quest.getQuestId());
        questData.put("maxLevels", quest.getMaxLevels());
        questData.put("databaseUpdateIntervalSeconds", quest.getDatabaseUpdateIntervalSeconds());

        CompletableFuture<Void> saveQuestInfoFuture = database.saveData("all_quests", quest.getQuestId(), questData);
        saveQuestInfoFuture.thenAccept(result -> future.complete(null))
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                });

        return future;
    }

    public CompletableFuture<Void> loadAllQuestsToHashMap() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture<List<Map<String, Object>>> allQuestsDataFuture = database.getAllDataFrom("all_quests");
        allQuestsDataFuture.thenAccept(allQuestsData -> {
            if (allQuestsData != null) {
                quests.clear();

                for (Map<String, Object> questData : allQuestsData) {
                    String questId = (String) questData.get("questId");
                    int maxLevels = (int) questData.get("maxLevels");
                    int databaseUpdateIntervalSeconds = (int) questData.get("databaseUpdateIntervalSeconds");

                    Quest quest = new Quest(questId, maxLevels, database, databaseUpdateIntervalSeconds);
                    quests.put(questId, quest);
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