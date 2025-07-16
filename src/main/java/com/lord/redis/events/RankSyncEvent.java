package com.lord.redis.events;

import com.lord.rank.Rank;
import lombok.Getter;

@Getter
public class RankSyncEvent extends RedisEvent {
    
    public enum Action {
        CREATE, UPDATE, DELETE
    }
    
    private final Action action;
    private final String rankName;
    private final Rank rank; // null for DELETE action
    
    public RankSyncEvent(String sourceServerId, Action action, String rankName, Rank rank) {
        super(sourceServerId);
        this.action = action;
        this.rankName = rankName;
        this.rank = rank;
    }
}