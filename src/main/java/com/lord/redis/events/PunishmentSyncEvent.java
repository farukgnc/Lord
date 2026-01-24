package com.lord.redis.events;

import com.lord.punishment.Punishment;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PunishmentSyncEvent extends RedisEvent {

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    private final Action action;
    private final UUID punishmentId;
    private final UUID playerUuid;
    private final String targetName;
    private final String issuerName;
    private final Punishment punishment; // null for DELETE action

    public PunishmentSyncEvent(String sourceServerId, Action action, UUID punishmentId, UUID playerUuid,
            String targetName, String issuerName, Punishment punishment) {
        super(sourceServerId);
        this.action = action;
        this.punishmentId = punishmentId;
        this.playerUuid = playerUuid;
        this.targetName = targetName;
        this.issuerName = issuerName;
        this.punishment = punishment;
    }
}