package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.redis.RedisService;
import com.lord.redis.events.RankSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankSyncServiceTest {

    private ServiceRegistry registry;
    private RedisService redisService;
    private RankRepository rankRepository;
    private PlayerDataCache playerDataCache;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        redisService = Mockito.mock(RedisService.class);
        rankRepository = Mockito.mock(RankRepository.class);
        playerDataCache = Mockito.mock(PlayerDataCache.class);

        Lord plugin = Mockito.mock(Lord.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        when(redisService.getServerId()).thenReturn("server-1");
        when(rankRepository.loadAllRanks()).thenReturn(CompletableFuture.completedFuture(Set.of()));
        when(playerDataCache.refreshPlayerDataCache()).thenReturn(CompletableFuture.completedFuture(null));

        registry.register(Lord.class, plugin);
        registry.register(RedisService.class, redisService);
        registry.register(RankRepository.class, rankRepository);
        registry.register(PlayerDataCache.class, playerDataCache);
    }

    @Test
    void shouldIgnoreEventsFromSameServer() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:rank:sync"), any(JedisPubSub.class));

        new RankSyncService(registry);

        RankSyncEvent event = new RankSyncEvent("server-1", RankSyncEvent.Action.UPDATE, "mod", new Rank("mod"));
        subscriberRef.get().onMessage("lord:rank:sync", RedisSerializer.serialize(event));

        verify(rankRepository, never()).loadAllRanks();
        verify(playerDataCache, never()).refreshPlayerDataCache();
    }

    @Test
    void shouldHandleRemoteEventByRefreshingCaches() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:rank:sync"), any(JedisPubSub.class));

        new RankSyncService(registry);

        RankSyncEvent event = new RankSyncEvent("server-2", RankSyncEvent.Action.DELETE, "mod", null);
        subscriberRef.get().onMessage("lord:rank:sync", RedisSerializer.serialize(event));

        verify(rankRepository, times(1)).loadAllRanks();
        verify(playerDataCache, times(1)).refreshPlayerDataCache();
    }

    @Test
    void broadcastMethodsShouldPublishToRedis() {
        RankSyncService service = new RankSyncService(registry);

        Rank rank = new Rank("admin");
        service.broadcastRankCreate(rank);
        service.broadcastRankUpdate(rank);
        service.broadcastRankDelete("admin");

        verify(redisService, times(3)).publish(eq("lord:rank:sync"), any(String.class));
    }
}
