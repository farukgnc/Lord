package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.Grant;
import com.lord.grant.GrantCacheService;
import com.lord.redis.RedisService;
import com.lord.redis.events.GrantSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.redis.utils.RedisKeys;
import com.lord.service.ServiceRegistry;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Logger;

public class GrantSyncService {
    
    private static final String CHANNEL = RedisKeys.GRANT_SYNC_CHANNEL;
    
    private final RedisService redisService;
    private final GrantCacheService grantCacheService;
    private final PlayerDataCache playerDataCache;

    private final Logger logger;
    private final String serverId;
    
    public GrantSyncService(ServiceRegistry serviceRegistry) {
        this.redisService = serviceRegistry.get(RedisService.class);
        this.grantCacheService = serviceRegistry.get(GrantCacheService.class);
        this.playerDataCache = serviceRegistry.get(PlayerDataCache.class);

        this.logger = serviceRegistry.get(Lord.class).getLogger();
        this.serverId = redisService.getServerId();
        
        setupSubscriber();
    }
    
    private void setupSubscriber() {
        redisService.subscribe(CHANNEL, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    GrantSyncEvent event = (GrantSyncEvent) RedisSerializer.deserialize(message);
                    
                    // Ignore events from our own server
                    if (event.getSourceServerId().equals(serverId)) {
                        return;
                    }
                    
                    handleSyncEvent(event);
                } catch (Exception e) {
                    logger.warning("Failed to handle grant sync event: " + e.getMessage());
                }
            }
        });
    }
    
    private void handleSyncEvent(GrantSyncEvent event) {
        switch (event.getAction()) {
            case CREATE, UPDATE -> {
                logger.info("Grant cache invalidated for player " + event.getPlayerUuid() + 
                           " due to " + event.getAction().name().toLowerCase() + " from " + event.getSourceServerId());
            }
            case DELETE -> {
                logger.info("Grant cache invalidated for player " + event.getPlayerUuid() + 
                           " due to deletion from " + event.getSourceServerId());
            }
        }

        grantCacheService.invalidate(event.getPlayerUuid());
        playerDataCache.refreshPlayerData(event.getPlayerUuid());
    }
    
    public void broadcastGrantCreate(Grant grant) {
        GrantSyncEvent event = new GrantSyncEvent(
            serverId,
            GrantSyncEvent.Action.CREATE,
            grant.getUniqueId(),
            grant.getGranteeUuid(),
            grant
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
    
    public void broadcastGrantUpdate(Grant grant) {
        GrantSyncEvent event = new GrantSyncEvent(
            serverId,
            GrantSyncEvent.Action.UPDATE,
            grant.getUniqueId(),
            grant.getGranteeUuid(),
            grant
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
    
    public void broadcastGrantDelete(UUID grantId, UUID playerUuid) {
        GrantSyncEvent event = new GrantSyncEvent(
            serverId,
            GrantSyncEvent.Action.DELETE,
            grantId,
            playerUuid,
            null
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
}