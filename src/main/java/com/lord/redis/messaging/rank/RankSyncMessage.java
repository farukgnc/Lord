package com.lord.redis.messaging.rank;

import com.lord.redis.messaging.PrefixedMessage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RankSyncMessage extends PrefixedMessage {

    private final UUID rankId;

    public RankSyncMessage(String serverId, UUID rankId) {
        super(serverId);
        this.rankId = rankId;
    }
}
