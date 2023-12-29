package nl.juriantech.questapi.interfaces;

import nl.juriantech.questapi.QuestAPI;
import nl.juriantech.questapi.objects.Quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseInterface {

    boolean connect(String connectionString, String databaseName);
    CompletableFuture<Void> saveData(String collection, String key, Object data);

    CompletableFuture<Void> updateData(String collection, String key, Object newData);

    CompletableFuture<Void> deleteData(String collection, String key);

    CompletableFuture<Void> saveQuestToDatabase(Quest quest);

    CompletableFuture<Map<String, Quest>> loadAllQuestsToHashMap(QuestAPI plugin);

    CompletableFuture<Void> levelUp(UUID playerUUID, String questId, int newLevel);

    CompletableFuture<Map<UUID, Integer>> loadPlayerProgress(String questId, QuestAPI plugin);
}