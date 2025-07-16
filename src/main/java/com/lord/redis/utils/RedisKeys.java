package com.lord.redis.utils;

import java.util.UUID;

/**
 * Utility class for generating consistent Redis keys across the application
 */
public class RedisKeys {
    
    private static final String PREFIX = "lord:";
    
    // Channel names for pub/sub
    public static final String PUNISHMENT_SYNC_CHANNEL = PREFIX + "punishment:sync";
    public static final String GRANT_SYNC_CHANNEL = PREFIX + "grant:sync";
    public static final String RANK_SYNC_CHANNEL = PREFIX + "rank:sync";
    
    // Cache keys
    public static String playerPunishments(UUID playerUuid) {
        return PREFIX + "cache:punishments:" + playerUuid.toString();
    }
    
    public static String playerGrants(UUID playerUuid) {
        return PREFIX + "cache:grants:" + playerUuid.toString();
    }
    
    public static String playerData(UUID playerUuid) {
        return PREFIX + "cache:playerdata:" + playerUuid.toString();
    }
    
    public static String rankData(String rankName) {
        return PREFIX + "cache:rank:" + rankName;
    }
    
    // Server status keys
    public static String serverStatus(String serverId) {
        return PREFIX + "server:status:" + serverId;
    }
    
    public static String serverHeartbeat(String serverId) {
        return PREFIX + "server:heartbeat:" + serverId;
    }
}