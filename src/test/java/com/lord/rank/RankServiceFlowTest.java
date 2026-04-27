package com.lord.rank;

import com.lord.Lord;
import com.lord.rank.repositories.RankRepository;
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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankServiceFlowTest {

    private ServiceRegistry registry;
    private RankRepository rankRepository;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        rankRepository = Mockito.mock(RankRepository.class);

        registry.register(Lord.class, Mockito.mock(Lord.class));
        registry.register(RankRepository.class, rankRepository);
    }

    @Test
    void updateRankShouldReturnFalseWhenRankMissing() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        when(rankRepository.findByName("missing")).thenReturn(Optional.empty());

        RankService service = new RankService(registry);
        boolean result = service.updateRank("missing", 10, null, null, null, sender).join();

        assertThat(result).isFalse();
        verify(sender, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void updateRankShouldSaveAndBroadcastWhenChanged() {
        Player updater = Mockito.mock(Player.class);
        when(updater.getName()).thenReturn("Updater");

        Rank rank = new Rank("mod");
        rank.setPriority(1);

        when(rankRepository.findByName("mod")).thenReturn(Optional.of(rank));
        when(rankRepository.save(rank)).thenReturn(CompletableFuture.completedFuture(true));

        RankService service = new RankService(registry);

        try (MockedStatic<Bukkit> bukkit = mockBukkitScheduler()) {
            boolean result = service.updateRank("mod", 50, "[M]", null, Set.of(), updater).join();

            assertThat(result).isTrue();
            assertThat(rank.getPriority()).isEqualTo(50);
            verify(rankRepository).save(rank);
            bukkit.verify(() -> Bukkit.broadcast(any(Component.class)), times(1));
        }
    }

    @Test
    void deleteRankShouldBroadcastWhenSuccess() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        Rank rank = new Rank("vip");

        when(rankRepository.findByName("vip")).thenReturn(Optional.of(rank));
        when(rankRepository.delete("vip")).thenReturn(CompletableFuture.completedFuture(true));

        RankService service = new RankService(registry);

        try (MockedStatic<Bukkit> bukkit = mockBukkitScheduler()) {
            boolean result = service.deleteRank("vip", sender).join();

            assertThat(result).isTrue();
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
