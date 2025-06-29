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
        name = "unban",
        permission = "lord.command.unban",
        description = "Removes an active ban from a player.",
        usage = "/unban <player>"
)
public final class UnbanCommand implements ICommand {

    private final PunishmentModule punishmentModule;
    private final Lord plugin;

    public UnbanCommand(ServiceRegistry registry) {
        this.punishmentModule = registry.get(PunishmentModule.class);
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() != 1) {
            sender.sendMessage(Component.text("Usage: /unban <player>", NamedTextColor.RED));
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
                this.punishmentModule.pardonPunishment(PunishmentType.BAN, targetUuid, targetName, sender);
            });
        });
    }
}