package com.lord.redis.messaging.broadcast;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.redis.sync.RedisBroadcastService;
import com.lord.services.ServiceRegistry;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class BroadcastListener {

    private final ServiceRegistry registry;
    private final MiniMessage miniMessage;

    public BroadcastListener(ServiceRegistry registry) {
        this.registry = registry;
        this.miniMessage = MiniMessage.miniMessage();
        this.registerListener();
    }

    private void registerListener() {
        RedisBroadcastService broadcastService = registry.get(RedisBroadcastService.class);

        broadcastService.subscribe(BroadcastMessage.class, message -> {
            Bukkit.broadcast(miniMessage.deserialize(message.getMessage()));
        });
    }
}
