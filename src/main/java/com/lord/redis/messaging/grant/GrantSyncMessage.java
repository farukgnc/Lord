package com.lord.redis.messaging.grant;

import com.lord.redis.messaging.PrefixedMessage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class GrantSyncMessage extends PrefixedMessage {

    private final UUID grantId;

    public GrantSyncMessage(String serverId, UUID grantId) {
        super(serverId);
        this.grantId = grantId;
    }
}
