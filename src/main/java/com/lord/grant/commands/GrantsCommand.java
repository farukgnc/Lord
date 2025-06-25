package com.lord.grant.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.menu.MenuManager;
import com.lord.grant.menu.GrantsMenu; // Yeni menümüzü import ediyoruz
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(
        name = "grants",
        permission = "lord.command.grants",
        description = "Shows the grants of a player in a menu.",
        usage = "/grants <player>"
)
public final class GrantsCommand implements ICommand {

    private final ServiceRegistry registry;
    private final MenuManager menuManager;

    public GrantsCommand(ServiceRegistry registry) {
        this.registry = registry;
        this.menuManager = registry.get(MenuManager.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        // Menüler sadece oyuncular tarafından açılabilir.
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return;
        }

        if (context.length() != 1) {
            player.sendMessage(Component.text("Usage: /grants <player>", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }

        // Yeni menü nesnesini oluştur ve oyuncuya aç.
        this.menuManager.open(player, new GrantsMenu(target, this.registry));
    }
}