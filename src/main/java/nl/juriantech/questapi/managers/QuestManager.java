package nl.juriantech.questapi.managers;

import nl.juriantech.questapi.QuestAPI;
import nl.juriantech.questapi.interfaces.DatabaseInterface;
import nl.juriantech.questapi.objects.Quest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuestManager {

    private final QuestAPI plugin;
    private final DatabaseInterface database;
    private Map<String, Quest> quests;

    public QuestManager(QuestAPI plugin, DatabaseInterface database) {
        this.plugin = plugin;
        this.database = database;
        this.quests = new HashMap<>();
    }

    public void addQuest(Quest quest) {
        quests.put(quest.getQuestId(), quest);
        database.saveQuestToDatabase(quest);
    }

    public Quest getQuest(String questId) {
        return quests.get(questId);
    }

    public CompletableFuture<Void> saveAllQuestsToDatabase() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        for (Quest quest : quests.values()) {
            database.saveQuestToDatabase(quest)
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                });
        }

        future.complete(null);
        return future;
    }

    public CompletableFuture<Void> loadAllQuestsToHashMap() {
        CompletableFuture<Map<String, Quest>> loadQuestsFuture = database.loadAllQuestsToHashMap(plugin);

        return loadQuestsFuture.thenAcceptAsync(loadedQuests -> {
            quests = loadedQuests;
        }).thenApplyAsync(__ -> null);
    }


    public Map<String, Quest> getAllQuests() {
        return quests;
    }
}