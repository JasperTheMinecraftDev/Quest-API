package nl.juriantech.questapi;

import nl.juriantech.questapi.impl.DatabaseImplementationMongoDB;
import nl.juriantech.questapi.interfaces.DatabaseInterface;
import nl.juriantech.questapi.managers.QuestManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class Quest_API extends JavaPlugin {

    private String connectionURL = "mongodb+srv://test:<redacted>/?retryWrites=true&w=majority";
    private DatabaseInterface database;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        database = new DatabaseImplementationMongoDB();
        database.connect(connectionURL, "test");

        questManager = new QuestManager(database);
        CompletableFuture<Void> loadQuestsFuture = questManager.loadAllQuestsToHashMap();
        loadQuestsFuture.exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    @Override
    public void onDisable() {
        // Save all quests to the database before shutdown
        CompletableFuture<Void> saveQuestsFuture = questManager.saveAllQuestsToDatabase();
        saveQuestsFuture.exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    public DatabaseInterface getDatabase() {
        return database;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}
