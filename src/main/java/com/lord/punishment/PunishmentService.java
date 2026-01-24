package com.lord.punishment;

import com.lord.Lord;
import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.enums.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.redis.sync.PunishmentSyncService;
import com.lord.service.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentService {

    private final Lord plugin;

    private final PunishmentRepository punishmentRepository;
    private final PunishmentCacheService punishmentCacheService;
    private PunishmentSyncService punishmentSyncService;

    public PunishmentService(ServiceRegistry registry) {
        this.plugin = registry.get(Lord.class);

        this.punishmentRepository = registry.get(PunishmentRepository.class);
        this.punishmentCacheService = registry.get(PunishmentCacheService.class);

        // Initialize Redis sync service if available
        try {
            this.punishmentSyncService = registry.get(PunishmentSyncService.class);
        } catch (Exception e) {
            // Redis sync service not available, continue without it
            this.punishmentSyncService = null;
        }
    }

    /**
     * Executes a punishment on a player
     */
    public void executePunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender issuer,
            Duration duration, String reason) {
        CompletableFuture<Boolean> checkFuture = (type == PunishmentType.BAN || type == PunishmentType.MUTE)
                ? punishmentRepository.findWithFilters(targetUuid, PunishmentStatusFilter.ACTIVE, type)
                        .thenApply(list -> !list.isEmpty())
                : CompletableFuture.completedFuture(false);

        checkFuture.thenAcceptAsync(alreadyPunished -> {
            if (alreadyPunished) {
                runOnMainThread(() -> issuer.sendMessage(Component.text(
                        targetName + " already has an active " + type.name().toLowerCase() + ".", NamedTextColor.RED)));
                return;
            }

            Punishment punishment = new Punishment(type, targetUuid, reason,
                    (issuer instanceof Player p) ? p.getUniqueId() : null, duration);

            this.punishmentRepository.save(punishment).thenRun(() -> {
                this.punishmentCacheService.invalidate(targetUuid);

                // Broadcast punishment creation to other servers via Redis
                if (punishmentSyncService != null) {
                    punishmentSyncService.broadcastPunishmentCreate(punishment, targetName, issuer.getName());
                }
            });

            runOnMainThread(() -> performPunishmentActions(punishment, targetName, issuer.getName()));
        });
    }

    /**
     * Pardons a punishment for a player
     */
    public void pardonPunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender pardoner) {
        this.punishmentRepository.findWithFilters(targetUuid, PunishmentStatusFilter.ACTIVE, type)
                .thenAcceptAsync(activePunishments -> {
                    if (activePunishments.isEmpty()) {
                        runOnMainThread(() -> pardoner.sendMessage(Component.text(
                                targetName + " does not have an active " + type.name().toLowerCase() + ".",
                                NamedTextColor.RED)));
                        return;
                    }

                    UUID pardonerUuid = (pardoner instanceof Player p) ? p.getUniqueId() : null;

                    for (Punishment punishmentToPardon : activePunishments) {
                        punishmentToPardon.setPardoned(true);
                        punishmentToPardon.setPardonerUuid(pardonerUuid);
                        punishmentToPardon.setPardonTime(Instant.now());

                        // Save updated punishment to database
                        this.punishmentRepository.save(punishmentToPardon);

                        // Broadcast punishment update to other servers via Redis
                        if (punishmentSyncService != null) {
                            String pardonerName = (pardoner instanceof Player) ? pardoner.getName() : "Console";
                            punishmentSyncService.broadcastPunishmentUpdate(punishmentToPardon, targetName,
                                    pardonerName);
                        }
                    }

                    this.punishmentCacheService.invalidate(targetUuid);

                    runOnMainThread(() -> {
                        String pardonerName = (pardoner instanceof Player) ? pardoner.getName() : "Console";
                        String verb = "un" + type.getPastTense();
                        Component broadcastMessage = MiniMessage.miniMessage().deserialize(
                                "<green><b>PARDON</b></green> <gray>»</gray> <white><target></white> was <verb> by <white><pardoner></white>.",
                                Placeholder.unparsed("target", targetName),
                                Placeholder.unparsed("verb", verb),
                                Placeholder.unparsed("pardoner", pardonerName));
                        Bukkit.broadcast(broadcastMessage);
                    });
                });
    }

    /**
     * Gets all punishments for a player
     */
    public CompletableFuture<List<Punishment>> getPlayerPunishments(UUID playerUuid) {
        return punishmentCacheService.getPunishments(playerUuid);
    }

    /**
     * Gets active punishments for a player with optional type filter
     */
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUuid, PunishmentType type) {
        return punishmentRepository.findWithFilters(playerUuid, PunishmentStatusFilter.ACTIVE, type);
    }

    /**
     * Checks if a player has an active punishment of a specific type
     */
    public CompletableFuture<Boolean> hasActivePunishment(UUID playerUuid, PunishmentType type) {
        return getActivePunishments(playerUuid, type).thenApply(punishments -> !punishments.isEmpty());
    }

    /**
     * Invalidates the punishment cache for a player
     */
    public void invalidateCache(UUID playerUuid) {
        punishmentCacheService.invalidate(playerUuid);
    }

    public void performPunishmentActions(Punishment punishment, String targetName, String issuerName) {
        runOnMainThread(() -> {
            if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.KICK) {
                Player onlineTarget = Bukkit.getPlayer(punishment.getPunishedUuid());
                if (onlineTarget != null) {
                    String kickReason = "You have been " + punishment.getType().getPastTense() + ".\n" +
                            "Reason: " + punishment.getReason() + "\n" +
                            (punishment.getType() == PunishmentType.BAN
                                    ? "Expires: " + TimeUtil
                                            .formatDuration(Duration.between(Instant.now(), punishment.getExpiry()))
                                    : "");
                    onlineTarget.kick(Component.text(kickReason));
                }
            }

            String verb = punishment.getType().getPastTense();
            Component broadcastMessage = MiniMessage.miniMessage().deserialize(
                    "<red><b><type></b></red> <gray>»</gray> <white><target></white> was <verb> by <white><issuer></white>.",
                    Placeholder.unparsed("type", punishment.getType().name()),
                    Placeholder.unparsed("verb", verb),
                    Placeholder.unparsed("target", targetName),
                    Placeholder.unparsed("issuer", issuerName));
            Bukkit.broadcast(broadcastMessage);
        });
    }

    private void runOnMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(this.plugin, task);
    }
}