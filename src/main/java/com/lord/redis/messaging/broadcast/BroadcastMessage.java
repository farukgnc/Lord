package com.lord.redis.messaging.broadcast;

import com.lord.redis.messaging.PrefixedMessage;
import lombok.Getter;

@Getter
public class BroadcastMessage extends PrefixedMessage {

    private final String message;

    public BroadcastMessage(String serverId, String message) {
        super(serverId);
        this.message = message;
    }
}
