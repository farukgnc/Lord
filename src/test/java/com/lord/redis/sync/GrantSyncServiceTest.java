package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.Grant;
import com.lord.grant.GrantCacheService;
import com.lord.redis.RedisService;
import com.lord.redis.events.GrantSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.service.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantSyncServiceTest {

    private ServiceRegistry registry;
    private RedisService redisService;
    private GrantCacheService grantCacheService;
    private PlayerDataCache playerDataCache;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        redisService = Mockito.mock(RedisService.class);
        grantCacheService = Mockito.mock(GrantCacheService.class);
        playerDataCache = Mockito.mock(PlayerDataCache.class);

        Lord plugin = Mockito.mock(Lord.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        when(redisService.getServerId()).thenReturn("server-1");

        registry.register(Lord.class, plugin);
        registry.register(RedisService.class, redisService);
        registry.register(GrantCacheService.class, grantCacheService);
        registry.register(PlayerDataCache.class, playerDataCache);
    }

    @Test
    void shouldIgnoreEventsFromSameServer() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        when(playerDataCache.refreshPlayerData(any())).thenReturn(CompletableFuture.completedFuture(null));

        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:grant:sync"), any(JedisPubSub.class));

        new GrantSyncService(registry);

        Grant grant = new Grant(UUID.randomUUID(), "mod", null, Duration.ZERO);
        GrantSyncEvent sameServerEvent = new GrantSyncEvent(
                "server-1",
                GrantSyncEvent.Action.CREATE,
                grant.getUniqueId(),
                grant.getGranteeUuid(),
                "Notch",
                "Console",
                grant
        );

        subscriberRef.get().onMessage("lord:grant:sync", RedisSerializer.serialize(sameServerEvent));

        verify(grantCacheService, never()).invalidate(any());
        verify(playerDataCache, never()).refreshPlayerData(any());
    }

    @Test
    void shouldHandleRemoteCreateEventAndBroadcast() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        when(playerDataCache.refreshPlayerData(any())).thenReturn(CompletableFuture.completedFuture(null));

        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:grant:sync"), any(JedisPubSub.class));

        new GrantSyncService(registry);

        Grant grant = new Grant(UUID.randomUUID(), "admin", null, Duration.ofHours(1));
        GrantSyncEvent remoteEvent = new GrantSyncEvent(
                "server-2",
                GrantSyncEvent.Action.CREATE,
                grant.getUniqueId(),
                grant.getGranteeUuid(),
                "Notch",
                "Console",
                grant
        );

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.broadcast(any(Component.class))).thenReturn(0);
            when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(1);
                task.run();
                return Mockito.mock(BukkitTask.class);
            });

            subscriberRef.get().onMessage("lord:grant:sync", RedisSerializer.serialize(remoteEvent));

            verify(grantCacheService, times(1)).invalidate(grant.getGranteeUuid());
            verify(playerDataCache, times(1)).refreshPlayerData(grant.getGranteeUuid());
            bukkit.verify(() -> Bukkit.broadcast(any(Component.class)), times(1));
        }
    }

    @Test
    void broadcastMethodsShouldPublishToRedis() {
        GrantSyncService service = new GrantSyncService(registry);

        Grant grant = new Grant(UUID.randomUUID(), "mod", null, Duration.ZERO);
        service.broadcastGrantCreate(grant, "Notch", "Console");
        service.broadcastGrantUpdate(grant, "Notch", "Console");
        service.broadcastGrantDelete(UUID.randomUUID(), UUID.randomUUID(), "Notch", "Console");

        verify(redisService, times(3)).publish(eq("lord:grant:sync"), any(String.class));
    }
}
