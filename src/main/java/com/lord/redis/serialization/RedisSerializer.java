package com.lord.redis.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lord.redis.events.RedisEvent;
import com.lord.redis.events.PunishmentSyncEvent;
import com.lord.redis.events.GrantSyncEvent;
import com.lord.redis.events.RankSyncEvent;

import java.time.Duration;
import java.time.Instant;

public class RedisSerializer {
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .create();
    
    public static String serialize(RedisEvent event) {
        EventWrapper wrapper = new EventWrapper();
        wrapper.type = event.getClass().getSimpleName();
        wrapper.data = GSON.toJson(event);
        return GSON.toJson(wrapper);
    }
    
    public static RedisEvent deserialize(String json) {
        EventWrapper wrapper = GSON.fromJson(json, EventWrapper.class);
        
        return switch (wrapper.type) {
            case "PunishmentSyncEvent" -> GSON.fromJson(wrapper.data, PunishmentSyncEvent.class);
            case "GrantSyncEvent" -> GSON.fromJson(wrapper.data, GrantSyncEvent.class);
            case "RankSyncEvent" -> GSON.fromJson(wrapper.data, RankSyncEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + wrapper.type);
        };
    }
    
    private static class EventWrapper {
        String type;
        String data;
    }
}