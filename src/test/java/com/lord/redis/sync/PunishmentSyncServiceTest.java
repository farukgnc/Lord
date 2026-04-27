package com.lord.redis.sync;

import com.lord.Lord;
import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentCacheService;
import com.lord.punishment.PunishmentService;
import com.lord.punishment.enums.PunishmentType;
import com.lord.redis.RedisService;
import com.lord.redis.events.PunishmentSyncEvent;
import com.lord.redis.serialization.RedisSerializer;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PunishmentSyncServiceTest {

    private ServiceRegistry registry;
    private RedisService redisService;
    private PunishmentCacheService cacheService;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        redisService = Mockito.mock(RedisService.class);
        cacheService = Mockito.mock(PunishmentCacheService.class);

        Lord plugin = Mockito.mock(Lord.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        when(redisService.getServerId()).thenReturn("server-1");

        registry.register(Lord.class, plugin);
        registry.register(RedisService.class, redisService);
        registry.register(PunishmentCacheService.class, cacheService);
    }

    @Test
    void shouldIgnoreEventsFromSameServer() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:punishment:sync"), any(JedisPubSub.class));

        new PunishmentSyncService(registry);

        Punishment punishment = new Punishment(PunishmentType.BAN, UUID.randomUUID(), "reason", null, Duration.ZERO);
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                "server-1",
                PunishmentSyncEvent.Action.CREATE,
                punishment.getUniqueId(),
                punishment.getPunishedUuid(),
                "Notch",
                "Console",
                punishment
        );

        subscriberRef.get().onMessage("lord:punishment:sync", RedisSerializer.serialize(event));

        verify(cacheService, never()).invalidate(any());
    }

    @Test
    void shouldHandleRemoteCreateAndCallPunishmentService() {
        AtomicReference<JedisPubSub> subscriberRef = new AtomicReference<>();
        PunishmentService punishmentService = Mockito.mock(PunishmentService.class);
        registry.register(PunishmentService.class, punishmentService);

        Mockito.doAnswer(invocation -> {
            subscriberRef.set(invocation.getArgument(1));
            return null;
        }).when(redisService).subscribe(eq("lord:punishment:sync"), any(JedisPubSub.class));

        new PunishmentSyncService(registry);

        Punishment punishment = new Punishment(PunishmentType.MUTE, UUID.randomUUID(), "spam", null, Duration.ofHours(1));
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                "server-2",
                PunishmentSyncEvent.Action.CREATE,
                punishment.getUniqueId(),
                punishment.getPunishedUuid(),
                "Notch",
                "Console",
                punishment
        );

        subscriberRef.get().onMessage("lord:punishment:sync", RedisSerializer.serialize(event));

        verify(cacheService, times(1)).invalidate(punishment.getPunishedUuid());
        verify(punishmentService, times(1)).performPunishmentActions(any(Punishment.class), eq("Notch"), eq("Console"));
    }

    @Test
    void broadcastMethodsShouldPublishToRedis() {
        PunishmentSyncService service = new PunishmentSyncService(registry);

        Punishment punishment = new Punishment(PunishmentType.WARN, UUID.randomUUID(), "reason", null, Duration.ZERO);
        service.broadcastPunishmentCreate(punishment, "Notch", "Console");
        service.broadcastPunishmentUpdate(punishment, "Notch", "Console");
        service.broadcastPunishmentDelete(UUID.randomUUID(), UUID.randomUUID(), "Notch", "Console");

        verify(redisService, times(3)).publish(eq("lord:punishment:sync"), any(String.class));
    }
}
