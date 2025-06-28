package com.lord.punishment;

import com.lord.Lord;
import com.lord.module.Module;
import com.lord.punishment.exceptions.CannotPunishSelfException;
import com.lord.punishment.exceptions.PlayerAlreadyPunishedException;
import com.lord.punishment.exceptions.PlayerNotPunishedException;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public final class PunishmentModule implements Module {

    private final ServiceRegistry registry;
    private final PunishmentRepository punishmentRepository;

    public PunishmentModule(ServiceRegistry registry) {
        this.registry = registry;
        this.punishmentRepository = registry.get(PunishmentRepository.class);
    }

    public Punishment executePunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender issuer, Duration duration, String reason)
            throws PlayerAlreadyPunishedException, CannotPunishSelfException {

        if (issuer instanceof Player && ((Player) issuer).getUniqueId().equals(targetUuid)) {
            throw new CannotPunishSelfException("You cannot punish yourself.");
        }

        if (type == PunishmentType.BAN || type == PunishmentType.MUTE) {
            if (!this.punishmentRepository.findActiveByType(targetUuid, type).isEmpty()) {
                throw new PlayerAlreadyPunishedException(targetName + " already has an active " + type.name().toLowerCase() + ".");
            }
        }

        UUID issuerUuid = (issuer instanceof Player player) ? player.getUniqueId() : null;

        Punishment punishment = new Punishment(type, targetUuid, reason, issuerUuid, duration);
        this.punishmentRepository.save(punishment);

        performPunishmentActions(punishment, targetName, issuer.getName());

        return punishment;
    }

    public void pardonPunishment(PunishmentType type, UUID targetUuid, String targetName, CommandSender pardoner)
            throws PlayerNotPunishedException {

        Set<Punishment> activePunishments = this.punishmentRepository.findActiveByType(targetUuid, type);

        if (activePunishments.isEmpty()) {
            throw new PlayerNotPunishedException(targetName + " does not have an active " + type.name().toLowerCase() + ".");
        }

        for (Punishment punishment : activePunishments) {
            this.punishmentRepository.delete(punishment);
        }

        String pardonerName = (pardoner instanceof Player) ? pardoner.getName() : "Console";
        String verb = "un" + type.getPastTense();

        Component broadcastMessage = MiniMessage.miniMessage().deserialize(
                "<green><b>PARDON</b></green> <gray>»</gray> <white><target></white> was <verb> by <white><pardoner></white>.",
                Placeholder.unparsed("target", targetName),
                Placeholder.unparsed("verb", verb),
                Placeholder.unparsed("pardoner", pardonerName)
        );
        Bukkit.broadcast(broadcastMessage);
    }

    private void performPunishmentActions(Punishment punishment, String targetName, String issuerName) {
        if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.KICK) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(punishment.getPunishedUuid());
            if (target.isOnline() && target.getPlayer() != null) {
                String kickReason = "You have been " + punishment.getType().getPastTense() + ".\n" +
                        "Reason: " + punishment.getReason() + "\n" +
                        (punishment.getType() == PunishmentType.BAN ? "Expires: " + TimeUtil.formatDuration(punishment.getDuration()) : "");
                target.getPlayer().kick(Component.text(kickReason));
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

    @Override
    public void enable() {
        Lord plugin = this.registry.get(Lord.class);
        Bukkit.getPluginManager().registerEvents(new PunishmentListener(this.registry), plugin);
        System.out.println("[" + getName() + "] module has been enabled, listeners are registered.");
    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "Punishment";
    }
}