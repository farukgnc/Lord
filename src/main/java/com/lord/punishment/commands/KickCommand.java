package com.lord.punishment.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.punishment.PunishmentService;
import com.lord.punishment.enums.PunishmentType;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Command(
        name = "kick",
        permission = "lord.command.kick",
        description = "Kicks a player from the server.",
        usage = "/kick <player> [reason...]"
)
public final class KickCommand implements ICommand {

    private final PunishmentService punishmentService;

    public KickCommand(ServiceRegistry registry) {
        this.punishmentService = registry.get(PunishmentService.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() < 1) {
            sender.sendMessage(Component.text("Usage: /kick <player> [reason...]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(context.arg(0));

        if (target == null) {
            sender.sendMessage(Component.text("Player not found or is not online: " + context.arg(0), NamedTextColor.RED));
            return;
        }

        String reason = "None";
        if (context.length() > 1) {
            reason = Arrays.stream(context.args(), 1, context.args().length)
                    .collect(Collectors.joining(" "));
        }

        this.punishmentService.executePunishment(PunishmentType.KICK, target.getUniqueId(), target.getName(), sender, Duration.ZERO, reason);
    }
}