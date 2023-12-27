package nl.juriantech.questapi.listeners;

import nl.juriantech.questapi.QuestAPI;
import nl.juriantech.questapi.objects.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeaveListener implements Listener {

    private final QuestAPI plugin;

    public PlayerLeaveListener(QuestAPI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (Quest quest : plugin.getQuestManager().getAllQuests().values()) {
            quest.updateDatabaseFromCache(player.getUniqueId());
        }
    }
}
