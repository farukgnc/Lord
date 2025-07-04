package com.lord.redis.pubsub;

import com.google.gson.Gson;
import com.lord.redis.Redis;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;

@RequiredArgsConstructor
public class RedisPublisher<T> {

    private final Redis redis;
    private final String channel;
    private final Gson gson = new Gson();

    public void publish(T message) {
        try (Jedis jedis = redis.getJedisPool().getResource()) {
            String jsonMessage = gson.toJson(message);
            jedis.publish(channel, jsonMessage);
        }
    }
}
