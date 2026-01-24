package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentCacheService;
import com.lord.punishment.PunishmentService;
import com.lord.redis.RedisService;
import com.lord.redis.events.PunishmentSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.redis.utils.RedisKeys;
import com.lord.service.ServiceRegistry;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Logger;

public class PunishmentSyncService {

    private static final String CHANNEL = RedisKeys.PUNISHMENT_SYNC_CHANNEL;

    private final RedisService redisService;
    private final PunishmentCacheService cacheService;
    private final ServiceRegistry serviceRegistry;
    private final Logger logger;
    private final String serverId;

    public PunishmentSyncService(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.redisService = serviceRegistry.get(RedisService.class);
        this.cacheService = serviceRegistry.get(PunishmentCacheService.class);
        this.logger = serviceRegistry.get(Lord.class).getLogger();
        this.serverId = redisService.getServerId();

        setupSubscriber();
    }

    private void setupSubscriber() {
        redisService.subscribe(CHANNEL, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    PunishmentSyncEvent event = (PunishmentSyncEvent) RedisSerializer.deserialize(message);

                    // Ignore events from our own server
                    if (event.getSourceServerId().equals(serverId)) {
                        return;
                    }

                    handleSyncEvent(event);
                } catch (Exception e) {
                    logger.warning("Failed to handle punishment sync event: " + e.getMessage());
                }
            }
        });
    }

    private void handleSyncEvent(PunishmentSyncEvent event) {
        // Invalidate cache to force refresh from database
        cacheService.invalidate(event.getPlayerUuid());

        // Perform local actions (kick/broadcast) if it's a new or updated punishment
        if (event.getAction() == PunishmentSyncEvent.Action.CREATE
                || event.getAction() == PunishmentSyncEvent.Action.UPDATE) {
            try {
                PunishmentService punishmentService = serviceRegistry.get(PunishmentService.class);
                punishmentService.performPunishmentActions(event.getPunishment(), event.getTargetName(),
                        event.getIssuerName());
            } catch (Exception e) {
                // PunishmentService might not be registered yet or other error
                logger.warning("Could not perform remote punishment actions: " + e.getMessage());
            }
        }

        logger.info("Punishment cache invalidated for player " + event.getPlayerUuid() +
                " due to " + event.getAction().name().toLowerCase() + " from " + event.getSourceServerId());
    }

    public void broadcastPunishmentCreate(Punishment punishment, String targetName, String issuerName) {
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                serverId,
                PunishmentSyncEvent.Action.CREATE,
                punishment.getUniqueId(),
                punishment.getPunishedUuid(),
                targetName,
                issuerName,
                punishment);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }

    public void broadcastPunishmentUpdate(Punishment punishment, String targetName, String issuerName) {
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                serverId,
                PunishmentSyncEvent.Action.UPDATE,
                punishment.getUniqueId(),
                punishment.getPunishedUuid(),
                targetName,
                issuerName,
                punishment);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }

    public void broadcastPunishmentDelete(UUID punishmentId, UUID playerUuid, String targetName, String issuerName) {
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                serverId,
                PunishmentSyncEvent.Action.DELETE,
                punishmentId,
                playerUuid,
                targetName,
                issuerName,
                null);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
}