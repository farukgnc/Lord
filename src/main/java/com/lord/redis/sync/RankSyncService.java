package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.rank.Rank;
import com.lord.redis.RedisService;
import com.lord.redis.events.RankSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.redis.utils.RedisKeys;
import com.lord.services.ServiceRegistry;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Logger;

public class RankSyncService {
    
    private static final String CHANNEL = RedisKeys.RANK_SYNC_CHANNEL;
    
    private final RedisService redisService;
    private final Logger logger;
    private final String serverId;
    
    public RankSyncService(ServiceRegistry serviceRegistry) {
        this.redisService = serviceRegistry.get(RedisService.class);
        this.logger = serviceRegistry.get(Lord.class).getLogger();
        this.serverId = redisService.getServerId();
        
        setupSubscriber();
    }
    
    private void setupSubscriber() {
        redisService.subscribe(CHANNEL, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    RankSyncEvent event = (RankSyncEvent) RedisSerializer.deserialize(message);
                    
                    // Ignore events from our own server
                    if (event.getSourceServerId().equals(serverId)) {
                        return;
                    }
                    
                    handleSyncEvent(event);
                } catch (Exception e) {
                    logger.warning("Failed to handle rank sync event: " + e.getMessage());
                }
            }
        });
    }
    
    private void handleSyncEvent(RankSyncEvent event) {
        switch (event.getAction()) {
            case CREATE -> {
                logger.info("Rank '" + event.getRankName() + "' created on " + event.getSourceServerId());
                // Here you could invalidate rank caches or trigger rank reload
            }
            case UPDATE -> {
                logger.info("Rank '" + event.getRankName() + "' updated on " + event.getSourceServerId());
                // Here you could invalidate rank caches or trigger rank reload
            }
            case DELETE -> {
                logger.info("Rank '" + event.getRankName() + "' deleted on " + event.getSourceServerId());
                // Here you could invalidate rank caches or trigger rank reload
            }
        }
    }
    
    public void broadcastRankCreate(Rank rank) {
        RankSyncEvent event = new RankSyncEvent(
            serverId,
            RankSyncEvent.Action.CREATE,
            rank.getName(),
            rank
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
    
    public void broadcastRankUpdate(Rank rank) {
        RankSyncEvent event = new RankSyncEvent(
            serverId,
            RankSyncEvent.Action.UPDATE,
            rank.getName(),
            rank
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
    
    public void broadcastRankDelete(String rankName) {
        RankSyncEvent event = new RankSyncEvent(
            serverId,
            RankSyncEvent.Action.DELETE,
            rankName,
            null
        );
        
        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
}