package com.lord.redis.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class RedisEvent {
    private final String sourceServerId;
    private final long timestamp;
    
    public RedisEvent(String sourceServerId) {
        this.sourceServerId = sourceServerId;
        this.timestamp = System.currentTimeMillis();
    }
}