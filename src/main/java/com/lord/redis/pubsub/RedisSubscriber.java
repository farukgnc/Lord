package com.lord.redis.pubsub;

import com.google.gson.Gson;
import com.lord.redis.Redis;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Type;

@RequiredArgsConstructor
public abstract class RedisSubscriber<T> extends JedisPubSub {

    private final Redis redis;
    private final String channel;
    private final Gson gson = new Gson();
    private final Type type;

    public void subscribe() {
        new Thread(() -> {
            try (Jedis jedis = redis.getJedisPool().getResource()) {
                jedis.subscribe(this, channel);
            }
        }).start();
    }

    @Override
    public void onMessage(String channel, String message) {
        if (this.channel.equals(channel)) {
            T data = gson.fromJson(message, type);
            onReceive(data);
        }
    }

    public abstract void onReceive(T data);
}
