package com.lord.punishment.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.punishment.PunishmentModule;
import com.lord.punishment.enums.PunishmentType;
import com.lord.services.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(
        name = "ban",
        permission = "lord.command.ban",
        description = "Bans a player from the server.",
        usage = "/ban <player> [duration] [reason...]"
)
public final class BanCommand implements ICommand {

    private final PunishmentModule punishmentModule;
    private final Lord plugin;

    public BanCommand(ServiceRegistry registry) {
        this.punishmentModule = registry.get(PunishmentModule.class);
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() < 1) {
            sender.sendMessage(Component.text("Usage: /ban <player> [duration] [reason...]", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        sender.sendMessage(Component.text("Searching for player '" + targetName + "'...", NamedTextColor.YELLOW));

        PlayerResolver.resolveUUID(targetName).thenAcceptAsync(targetUuidOpt -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (targetUuidOpt.isEmpty()) {
                    sender.sendMessage(Component.text("A player with the name '" + targetName + "' could not be found.", NamedTextColor.RED));
                    return;
                }

                UUID targetUuid = targetUuidOpt.get();
                Duration duration = Duration.ZERO;
                int reasonStartIndex = 1;
                String reason = "None";

                if (context.length() > 1) {
                    Optional<Duration> durationOpt = TimeUtil.parseDuration(context.arg(1));
                    if (durationOpt.isPresent()) {
                        duration = durationOpt.get();
                        reasonStartIndex = 2;
                    }
                }

                if (context.length() > reasonStartIndex) {
                    reason = Arrays.stream(context.args(), reasonStartIndex, context.args().length)
                            .collect(Collectors.joining(" "));
                }

                this.punishmentModule.executePunishment(PunishmentType.BAN, targetUuid, targetName, sender, duration, reason);
            });
        });
    }
}