package com.lord.punishment;

import com.lord.Lord;
import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.enums.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.service.ServiceRegistry;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PunishmentServiceTest {

    private ServiceRegistry registry;
    private PunishmentRepository punishmentRepository;
    private PunishmentCacheService punishmentCacheService;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        punishmentRepository = Mockito.mock(PunishmentRepository.class);
        punishmentCacheService = Mockito.mock(PunishmentCacheService.class);

        Lord plugin = Mockito.mock(Lord.class);

        registry.register(Lord.class, plugin);
        registry.register(PunishmentRepository.class, punishmentRepository);
        registry.register(PunishmentCacheService.class, punishmentCacheService);
    }

    @Test
    void shouldNotSaveWhenPlayerAlreadyHasActivePunishment() {
        UUID target = UUID.randomUUID();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Punishment activeBan = new Punishment(PunishmentType.BAN, target, "reason", null, Duration.ofHours(1));

        when(punishmentRepository.findWithFilters(target, PunishmentStatusFilter.ACTIVE, PunishmentType.BAN))
                .thenReturn(CompletableFuture.completedFuture(List.of(activeBan)));

        try (MockedStatic<Bukkit> bukkit = mockBukkitScheduler()) {
            PunishmentService service = new PunishmentService(registry);
            service.executePunishment(PunishmentType.BAN, target, "Notch", sender, Duration.ofHours(1), "reason");

            verify(punishmentRepository, never()).save(any(Punishment.class));
        }
    }

    @Test
    void shouldSaveAndExecuteActionsWhenNotAlreadyPunished() {
        UUID target = UUID.randomUUID();
        CommandSender sender = Mockito.mock(CommandSender.class);

        when(sender.getName()).thenReturn("Console");
        when(punishmentRepository.findWithFilters(target, PunishmentStatusFilter.ACTIVE, PunishmentType.MUTE))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(punishmentRepository.save(any(Punishment.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = mockBukkitScheduler()) {
            PunishmentService service = spy(new PunishmentService(registry));

            service.executePunishment(PunishmentType.MUTE, target, "Notch", sender, Duration.ofMinutes(10), "spam");

            verify(punishmentRepository, timeout(1000)).save(any(Punishment.class));
            verify(service, timeout(1000)).performPunishmentActions(any(Punishment.class), eq("Notch"), eq("Console"));
            verify(punishmentCacheService, timeout(1000)).invalidate(target);
        }
    }

    private MockedStatic<Bukkit> mockBukkitScheduler() {
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);

        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.broadcast(any(net.kyori.adventure.text.Component.class))).thenReturn(0);

        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return Mockito.mock(BukkitTask.class);
        });

        return bukkit;
    }
}
