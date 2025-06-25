package com.lord.grant.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.grant.menus.wizards.GrantWizard;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(
        name = "grant",
        permission = "lord.command.grant",
        description = "Assigns a rank to a player.",
        usage = "/grant <player>"
)
public final class GrantCommand implements ICommand {

    private final ServiceRegistry registry;

    public GrantCommand(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return;
        }

        if (context.length() != 1) {
            player.sendMessage(Component.text("Usage: /grant <player>", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }

        // Sihirbazı oluştur ve başlat!
        new GrantWizard(this.registry, player, target).start();
    }
}