package com.lord.redis.messaging.punishment;

import com.lord.redis.messaging.PrefixedMessage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PunishmentSyncMessage extends PrefixedMessage {

    private final UUID punishmentId;

    public PunishmentSyncMessage(String serverId, UUID punishmentId) {
        super(serverId);
        this.punishmentId = punishmentId;
    }
}
