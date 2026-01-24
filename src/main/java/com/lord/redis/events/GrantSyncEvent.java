package com.lord.redis.events;

import com.lord.grant.Grant;
import lombok.Getter;

import java.util.UUID;

@Getter
public class GrantSyncEvent extends RedisEvent {

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    private final Action action;
    private final UUID grantId;
    private final UUID playerUuid;
    private final String targetName;
    private final String issuerName;
    private final Grant grant; // null for DELETE action

    public GrantSyncEvent(String sourceServerId, Action action, UUID grantId, UUID playerUuid, String targetName,
            String issuerName, Grant grant) {
        super(sourceServerId);
        this.action = action;
        this.grantId = grantId;
        this.playerUuid = playerUuid;
        this.targetName = targetName;
        this.issuerName = issuerName;
        this.grant = grant;
    }
}