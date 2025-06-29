package com.lord.punishment.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.punishment.PunishmentModule;
import com.lord.punishment.PunishmentType;
import com.lord.services.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.UUID;

@Command(
        name = "unmute",
        permission = "lord.command.unmute",
        description = "Removes an active mute from a player.",
        usage = "/unmute <player>"
)
public final class UnmuteCommand implements ICommand {

    private final PunishmentModule punishmentModule;
    private final Lord plugin;

    public UnmuteCommand(ServiceRegistry registry) {
        this.punishmentModule = registry.get(PunishmentModule.class);
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() != 1) {
            sender.sendMessage(Component.text("Usage: /unmute <player>", NamedTextColor.RED));
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
                this.punishmentModule.pardonPunishment(PunishmentType.MUTE, targetUuid, targetName, sender);
            });
        });
    }
}