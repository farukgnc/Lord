package com.lord.redis.sync;

import com.google.gson.Gson;
import com.lord.config.impl.MainConfig;
import com.lord.redis.Redis;
import com.lord.redis.messaging.PrefixedMessage;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RedisSyncService extends JedisPubSub {

    private final String serverId;
    private final Redis redis;
    private final Gson gson = new Gson();
    private final Map<String, Consumer<String>> handlers = new HashMap<>();

    @Getter
    private static final String CHANNEL = "lord:sync";

    public RedisSyncService(ServiceRegistry serviceRegistry) {
        this.serverId = serviceRegistry.get(MainConfig.class).getServerId();
        this.redis = serviceRegistry.get(Redis.class);
        this.listen();
    }

    private void listen() {
        new Thread(() -> {
            try (Jedis jedis = redis.getJedisPool().getResource()) {
                jedis.subscribe(this, CHANNEL);
            }
        }).start();
    }

    public <T extends PrefixedMessage> void publish(T message) {
        try (Jedis jedis = redis.getJedisPool().getResource()) {
            jedis.publish(CHANNEL, gson.toJson(message));
        }
    }

    public <T extends PrefixedMessage> void subscribe(Class<T> messageType, Consumer<T> handler) {
        handlers.put(messageType.getName(), (json) -> {
            T message = gson.fromJson(json, messageType);
            if (!message.getServerId().equals(serverId)) {
                handler.accept(message);
            }
        });
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(CHANNEL)) {
            try {
                PrefixedMessage prefixedMessage = gson.fromJson(message, PrefixedMessage.class);
                Consumer<String> handler = handlers.get(prefixedMessage.getMessageType());
                if (handler != null) {
                    handler.accept(message);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
