package com.lord.redis.sync;

import com.lord.grant.Grant;
import com.lord.grant.GrantCacheService;
import com.lord.redis.RedisService;
import com.lord.redis.events.GrantSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.redis.utils.RedisKeys;
import com.lord.services.ServiceRegistry;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Logger;

public class GrantSyncService {
    
    private static final String CHANNEL = RedisKeys.GRANT_SYNC_CHANNEL;
    
    private final RedisService redisService;
    private final GrantCacheService cacheService;
    private final Logger logger;
    private final String serverId;
    
    public GrantSyncService(ServiceRegistry serviceRegistry) {
        this.redisService = serviceRegistry.get(RedisService.class);
        this.cacheService = serviceRegistry.get(GrantCacheService.class);
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
                // Invalidate cache to force refresh from database
                cacheService.invalidate(event.getPlayerUuid());
                logger.info("Grant cache invalidated for player " + event.getPlayerUuid() + 
                           " due to " + event.getAction().name().toLowerCase() + " from " + event.getSourceServerId());
            }
            case DELETE -> {
                // Invalidate cache to force refresh from database
                cacheService.invalidate(event.getPlayerUuid());
                logger.info("Grant cache invalidated for player " + event.getPlayerUuid() + 
                           " due to deletion from " + event.getSourceServerId());
            }
        }
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