package com.lord.data.playerdata;

import com.lord.Lord;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;

public final class PlayerDataListener implements Listener {

    private final PlayerDataCache playerDataCache;
    private final Field permissibleField;

    public PlayerDataListener(ServiceRegistry registry) {
        Lord plugin = registry.get(Lord.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);

        try {
            // Artık versiyonu dinamik olarak bulmaya çalışmıyoruz.
            // Modern Paper'ın kullandığı sabit ve versiyonsuz yolu direkt kullanıyoruz.
            Class<?> craftHumanEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity");
            this.permissibleField = craftHumanEntity.getDeclaredField("perm");
            this.permissibleField.setAccessible(true);

        } catch (ReflectiveOperationException e) {
            plugin.getLogger().severe("Could not inject permissible. This server version might not be compatible.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new RuntimeException("Failed to initialize PlayerDataListener", e);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        try {
            PermissibleBase oldPermissible = (PermissibleBase) this.permissibleField.get(player);
            PlayerPermissible newPermissible = new PlayerPermissible(player, this.playerDataCache, oldPermissible);
            this.permissibleField.set(player, newPermissible);
        } catch (Exception e) {
            e.printStackTrace();
            player.kick(Component.text("An error occurred while setting up your permissions."));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.playerDataCache.invalidate(event.getPlayer().getUniqueId());
    }
}
