package com.lord.punishment.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.punishment.PunishmentModule;
import com.lord.punishment.PunishmentType;
import com.lord.punishment.exceptions.CannotPunishSelfException;
import com.lord.punishment.exceptions.PlayerAlreadyPunishedException;
import com.lord.services.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(
        name = "warn",
        permission = "lord.command.warn",
        description = "Warns a player for a specific reason.",
        usage = "/warn <player> [reason...]"
)
public final class WarnCommand implements ICommand {

    private final PunishmentModule punishmentModule;
    private final Lord plugin;

    public WarnCommand(ServiceRegistry registry) {
        this.punishmentModule = registry.get(PunishmentModule.class);
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() < 1) {
            sender.sendMessage(Component.text("Usage: /warn <player> [reason...]", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        sender.sendMessage(Component.text("Searching for player '" + targetName + "'...", NamedTextColor.YELLOW));

        PlayerResolver.resolve(targetName).thenAcceptAsync(targetUuidOpt -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (targetUuidOpt.isEmpty()) {
                    sender.sendMessage(Component.text("A player with the name '" + targetName + "' could not be found.", NamedTextColor.RED));
                    return;
                }

                UUID targetUuid = targetUuidOpt.get();
                String reason = "None";
                if (context.length() > 1) {
                    reason = Arrays.stream(context.args(), 1, context.args().length)
                            .collect(Collectors.joining(" "));
                }

                try {
                    this.punishmentModule.executePunishment(PunishmentType.WARN, targetUuid, targetName, sender, Duration.ZERO, reason);
                } catch (CannotPunishSelfException | PlayerAlreadyPunishedException e) {
                    sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                }
            });
        });
    }
}