package com.lord.grant;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.service.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantServiceTest {

    private ServiceRegistry registry;
    private GrantRepository grantRepository;
    private GrantCacheService grantCacheService;
    private PlayerDataCache playerDataCache;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        grantRepository = Mockito.mock(GrantRepository.class);
        grantCacheService = Mockito.mock(GrantCacheService.class);
        playerDataCache = Mockito.mock(PlayerDataCache.class);

        Lord plugin = Mockito.mock(Lord.class);

        registry.register(Lord.class, plugin);
        registry.register(GrantRepository.class, grantRepository);
        registry.register(GrantCacheService.class, grantCacheService);
        registry.register(PlayerDataCache.class, playerDataCache);
    }

    @Test
    void shouldFailFutureWhenGrantSaveFails() {
        GrantService grantService = new GrantService(registry);
        CommandSender sender = Mockito.mock(CommandSender.class);

        when(grantRepository.save(any(Grant.class))).thenReturn(CompletableFuture.completedFuture(false));

        CompletableFuture<Grant> future = grantService.createGrant(
                UUID.randomUUID(),
                "Notch",
                "admin",
                sender,
                Duration.ZERO
        );

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasMessageContaining("Failed to save grant");

        verify(grantCacheService, never()).invalidate(any(UUID.class));
        verify(playerDataCache, never()).refreshPlayerData(any(UUID.class));
    }

    @Test
    void shouldInvalidateAndRefreshOnGrantCreateSuccess() {
        GrantService grantService = new GrantService(registry);
        CommandSender sender = Mockito.mock(CommandSender.class);
        UUID targetUuid = UUID.randomUUID();

        when(sender.getName()).thenReturn("Console");
        when(grantRepository.save(any(Grant.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(playerDataCache.refreshPlayerData(targetUuid)).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.broadcast(any(Component.class))).thenReturn(0);

            when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(1);
                task.run();
                return Mockito.mock(BukkitTask.class);
            });

            Grant grant = grantService.createGrant(targetUuid, "Notch", "admin", sender, Duration.ofHours(1)).join();

            assertThat(grant.getGranteeUuid()).isEqualTo(targetUuid);
            verify(grantCacheService, times(1)).invalidate(targetUuid);
            verify(playerDataCache, times(1)).refreshPlayerData(targetUuid);
        }
    }
}
