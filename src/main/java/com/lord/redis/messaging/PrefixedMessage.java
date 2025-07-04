package com.lord.redis.messaging;

import lombok.Getter;

@Getter
public class PrefixedMessage {

    private final String serverId;
    private final String messageType;

    public PrefixedMessage(String serverId) {
        this.serverId = serverId;
        this.messageType = this.getClass().getName();
    }
}
