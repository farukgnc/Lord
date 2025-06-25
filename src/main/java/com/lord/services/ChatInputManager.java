package com.lord.services;

import com.lord.Lord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatInputManager implements Listener {

    private final Lord plugin;

    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputManager(Lord plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void prompt(Player player, Consumer<String> onInput) {
        this.pendingInputs.put(player.getUniqueId(), onInput);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> onInput = this.pendingInputs.remove(player.getUniqueId());

        if (onInput == null) {
            return;
        }

        event.setCancelled(true);

        // Oyuncunun sohbet eylemini ana sunucu thread'inde çalıştırmak daha güvenlidir.
        Bukkit.getScheduler().runTask(plugin, () -> {
            onInput.accept(event.getMessage());
        });
    }
}
