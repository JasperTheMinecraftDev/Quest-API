package nl.juriantech.questapi.objects;

import nl.juriantech.questapi.interfaces.DatabaseInterface;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.*;

public class Quest {
    private final String questId;
    private final int maxLevels;
    private final DatabaseInterface database;
    private final Map<UUID, Integer> playerProgress;
    private final ScheduledExecutorService scheduler;
    private final int databaseUpdateIntervalSeconds;

    public Quest(String questId, int maxLevels, DatabaseInterface database, int databaseUpdateIntervalSeconds) {
        this.questId = questId;
        this.maxLevels = maxLevels;
        this.database = database;
        this.playerProgress = new ConcurrentHashMap<>();
        this.databaseUpdateIntervalSeconds = databaseUpdateIntervalSeconds;
        this.scheduler = Executors.newScheduledThreadPool(1);

        loadPlayerProgress();
        scheduleCacheUpdates();
    }

    public String getQuestId() {
        return questId;
    }

    public int getMaxLevels() {
        return maxLevels;
    }

    public int getDatabaseUpdateIntervalSeconds() {
        return databaseUpdateIntervalSeconds;
    }

    public Map<UUID, Integer> getPlayerProgress() {
        return playerProgress;
    }

    public CompletableFuture<Void> levelUp(Player player, int newLevel) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        playerProgress.put(player.getUniqueId(), newLevel);
        CompletableFuture<Void> updateFuture = database.updateData("player_quest_data", getKey(player), newLevel);
        updateFuture.thenAccept(result -> future.complete(null));
        updateFuture.exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }

    private void loadPlayerProgress() {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            CompletableFuture<Object> progressFuture = database.getData("player_quest_data", getKey(player));
            progressFuture.thenAccept(data -> {
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
    }


    private void scheduleCacheUpdates() {
        scheduler.scheduleAtFixedRate(() -> updateDatabaseFromCache(null), databaseUpdateIntervalSeconds, databaseUpdateIntervalSeconds, TimeUnit.SECONDS);
    }

    public void updateDatabaseFromCache(UUID specificPlayerUUID) {
        if (specificPlayerUUID != null) {
            OfflinePlayer specificPlayer = Bukkit.getOfflinePlayer(specificPlayerUUID);
            if (specificPlayer.isOnline()) {
                int currentLevel = playerProgress.getOrDefault(specificPlayerUUID, 0);
                database.updateData("player_quest_data", getKey(specificPlayer), currentLevel);
            }
        } else {
            playerProgress.forEach((playerUUID, level) -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                if (player.isOnline()) {
                    int currentLevel = playerProgress.getOrDefault(playerUUID, 0);
                    database.updateData("player_quest_data", getKey(player), currentLevel);
                }
            });
        }
    }

    public String getKey(OfflinePlayer player) {
        return questId + "_" + player.getUniqueId();
    }
}