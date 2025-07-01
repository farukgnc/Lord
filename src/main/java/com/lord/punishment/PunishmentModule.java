package com.lord.punishment;

import com.lord.Lord;
import com.lord.module.Module;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.services.ServiceRegistry;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PunishmentModule implements Module {

    private final ServiceRegistry registry;
    private final PunishmentRepository punishmentRepository;
    private final PunishmentCacheService punishmentCacheService;
    private final Lord plugin;

    public PunishmentModule(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.punishmentRepository = registry.get(PunishmentRepository.class);
        this.punishmentCacheService = registry.get(PunishmentCacheService.class);
    }

    public void executePunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender issuer, Duration duration, String reason) {
        /*if (issuer instanceof Player && ((Player) issuer).getUniqueId().equals(targetUuid)) {
            issuer.sendMessage(Component.text("You cannot punish yourself.", NamedTextColor.RED));
            return;
        }*/

        CompletableFuture<Boolean> checkFuture = (type == PunishmentType.BAN || type == PunishmentType.MUTE)
                ? punishmentRepository.findWithFilters(targetUuid, PunishmentStatusFilter.ACTIVE, type).thenApply(list -> !list.isEmpty())
                : CompletableFuture.completedFuture(false);

        checkFuture.thenAcceptAsync(alreadyPunished -> {
            if (alreadyPunished) {
                runOnMainThread(() -> issuer.sendMessage(Component.text(targetName + " already has an active " + type.name().toLowerCase() + ".", NamedTextColor.RED)));
                return;
            }

            Punishment punishment = new Punishment(type, targetUuid, reason, (issuer instanceof Player p) ? p.getUniqueId() : null, duration);

            this.punishmentRepository.save(punishment).thenRun(() -> {
                this.punishmentCacheService.invalidate(targetUuid);
            });

            runOnMainThread(() -> performPunishmentActions(punishment, targetName, issuer.getName()));
        });
    }

    public void pardonPunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender pardoner) {
        this.punishmentRepository.findWithFilters(targetUuid, PunishmentStatusFilter.ACTIVE, type).thenAcceptAsync(activePunishments -> {
            if (activePunishments.isEmpty()) {
                runOnMainThread(() -> pardoner.sendMessage(Component.text(targetName + " does not have an active " + type.name().toLowerCase() + ".", NamedTextColor.RED)));
                return;
            }

            UUID pardonerUuid = (pardoner instanceof Player p) ? p.getUniqueId() : null;

            for (Punishment punishmentToPardon : activePunishments) {
                punishmentToPardon.setPardoned(true);
                punishmentToPardon.setPardonerUuid(pardonerUuid);
                punishmentToPardon.setPardonTime(Instant.now());

                // Güncellenmiş ceza nesnesini veritabanına geri kaydet.
                this.punishmentRepository.save(punishmentToPardon);
            }

            this.punishmentCacheService.invalidate(targetUuid);

            runOnMainThread(() -> {
                String pardonerName = (pardoner instanceof Player) ? pardoner.getName() : "Console";
                String verb = "un" + type.getPastTense();
                Component broadcastMessage = MiniMessage.miniMessage().deserialize(
                        "<green><b>PARDON</b></green> <gray>»</gray> <white><target></white> was <verb> by <white><pardoner></white>.",
                        Placeholder.unparsed("target", targetName),
                        Placeholder.unparsed("verb", verb),
                        Placeholder.unparsed("pardoner", pardonerName)
                );
                Bukkit.broadcast(broadcastMessage);
            });
        });
    }

    private void performPunishmentActions(Punishment punishment, String targetName, String issuerName) {
        if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.KICK) {
            Player onlineTarget = Bukkit.getPlayer(punishment.getPunishedUuid());
            if (onlineTarget != null) {
                String kickReason = "You have been " + punishment.getType().getPastTense() + ".\n" +
                        "Reason: " + punishment.getReason() + "\n" +
                        (punishment.getType() == PunishmentType.BAN ? "Expires: " + TimeUtil.formatDuration(Duration.between(Instant.now(), punishment.getExpiry())) : "");
                onlineTarget.kick(Component.text(kickReason));
            }
        }

        String verb = punishment.getType().getPastTense();
        Component broadcastMessage = MiniMessage.miniMessage().deserialize(
                "<red><b><type></b></red> <gray>»</gray> <white><target></white> was <verb> by <white><issuer></white>.",
                Placeholder.unparsed("type", punishment.getType().name()),
                Placeholder.unparsed("verb", verb),
                Placeholder.unparsed("target", targetName),
                Placeholder.unparsed("issuer", issuerName)
        );
        Bukkit.broadcast(broadcastMessage);
    }

    private void runOnMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(this.plugin, task);
    }

    @Override
    public void enable() {
        this.registry.register(PunishmentModule.class, this);

        Bukkit.getPluginManager().registerEvents(new PunishmentListener(this.registry), this.plugin);

        System.out.println("[" + getName() + "] module has been enabled and listeners are registered.");
    }

    @Override
    public void disable() {
        this.registry.unregister(PunishmentModule.class);
    }

    @Override
    public String getName() {
        return "Punishment";
    }
}