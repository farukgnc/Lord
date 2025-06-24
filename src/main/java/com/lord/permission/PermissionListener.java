package com.lord.permission;

import com.lord.Lord;
import com.lord.services.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PermissionListener implements Listener {

    private final PermissionCache permissionCache;

    public PermissionListener(ServiceRegistry registry) {
        Lord plugin = registry.get(Lord.class);
        this.permissionCache = registry.get(PermissionCache.class);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        this.permissionCache.getPlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Artık ihtiyaç duyulmayan oyuncu verilerini önbellekten temizliyoruz.
        this.permissionCache.invalidate(player.getUniqueId());
    }
}