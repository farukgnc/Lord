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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
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
        grantCacheService.invalidate(event.getPlayerUuid());
        playerDataCache.refreshPlayerData(event.getPlayerUuid()).thenRun(() -> {
            // Show broadcast message on remote servers
            if (event.getAction() == GrantSyncEvent.Action.CREATE) {
                String durationStr = event.getGrant().getDuration().isZero() ? "permanently"
                        : "for a specific duration";

                Component message = MiniMessage.miniMessage().deserialize(
                        "<green><b>GRANT</b></green> <gray>»</gray> <white><target></white> was granted <yellow><rank></yellow> <duration> by <white><issuer></white> (via remote).",
                        Placeholder.unparsed("target", event.getTargetName()),
                        Placeholder.unparsed("rank", event.getGrant().getRankName()),
                        Placeholder.unparsed("duration", durationStr),
                        Placeholder.unparsed("issuer", event.getIssuerName()));
                Bukkit.broadcast(message);
            } else if (event.getAction() == GrantSyncEvent.Action.DELETE) {
                Component message = MiniMessage.miniMessage().deserialize(
                        "<red><b>GRANT REMOVED</b></red> <gray>»</gray> <white><target></white>'s grant was removed by <white><remover></white> (via remote).",
                        Placeholder.unparsed("target", event.getTargetName()),
                        Placeholder.unparsed("remover", event.getIssuerName()));
                Bukkit.broadcast(message);
            }
        });

        logger.info("Grant cache invalidated and player data refreshed for player " + event.getPlayerUuid() +
                " due to " + event.getAction().name().toLowerCase() + " from " + event.getSourceServerId());
    }

    public void broadcastGrantCreate(Grant grant, String targetName, String issuerName) {
        GrantSyncEvent event = new GrantSyncEvent(
                serverId,
                GrantSyncEvent.Action.CREATE,
                grant.getUniqueId(),
                grant.getGranteeUuid(),
                targetName,
                issuerName,
                grant);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }

    public void broadcastGrantUpdate(Grant grant, String targetName, String issuerName) {
        GrantSyncEvent event = new GrantSyncEvent(
                serverId,
                GrantSyncEvent.Action.UPDATE,
                grant.getUniqueId(),
                grant.getGranteeUuid(),
                targetName,
                issuerName,
                grant);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }

    public void broadcastGrantDelete(UUID grantId, UUID playerUuid, String targetName, String issuerName) {
        GrantSyncEvent event = new GrantSyncEvent(
                serverId,
                GrantSyncEvent.Action.DELETE,
                grantId,
                playerUuid,
                targetName,
                issuerName,
                null);

        String serialized = RedisSerializer.serialize(event);
        redisService.publish(CHANNEL, serialized);
    }
}