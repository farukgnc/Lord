package com.lord.grant;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.service.ServiceRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantServiceRemoveFlowTest {

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

        registry.register(Lord.class, Mockito.mock(Lord.class));
        registry.register(GrantRepository.class, grantRepository);
        registry.register(GrantCacheService.class, grantCacheService);
        registry.register(PlayerDataCache.class, playerDataCache);
    }

    @Test
    void removeGrantShouldReturnFalseWhenMissing() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        UUID grantId = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        when(grantRepository.findById(grantId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        GrantService service = new GrantService(registry);
        boolean result = service.removeGrant(grantId, player, "Notch", sender).join();

        assertThat(result).isFalse();
        verify(sender, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void removeGrantShouldInvalidateRefreshAndBroadcastWhenSuccess() {
        Player sender = Mockito.mock(Player.class);
        when(sender.getName()).thenReturn("Admin");

        UUID grantId = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        Grant grant = new Grant(grantId, player, "mod", null, java.time.Instant.now(), Duration.ZERO);

        when(grantRepository.findById(grantId)).thenReturn(CompletableFuture.completedFuture(Optional.of(grant)));
        when(grantRepository.delete(grant)).thenReturn(CompletableFuture.completedFuture(true));
        when(playerDataCache.refreshPlayerData(player)).thenReturn(CompletableFuture.completedFuture(null));

        GrantService service = new GrantService(registry);

        try (MockedStatic<Bukkit> bukkit = mockBukkitScheduler()) {
            boolean result = service.removeGrant(grantId, player, "Notch", sender).join();

            assertThat(result).isTrue();
            verify(grantCacheService).invalidate(player);
            verify(playerDataCache).refreshPlayerData(player);
            bukkit.verify(() -> Bukkit.broadcast(any(Component.class)), times(1));
        }
    }

    private MockedStatic<Bukkit> mockBukkitScheduler() {
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);

        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.broadcast(any(Component.class))).thenReturn(0);

        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return Mockito.mock(BukkitTask.class);
        });

        return bukkit;
    }
}
