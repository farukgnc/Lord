package com.lord.grant.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.grant.menus.GrantsMenu;
import com.lord.grant.GrantCacheService;
import com.lord.menu.MenuManager;
import com.lord.service.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Command(
        name = "grants",
        permission = "lord.command.grants",
        description = "Shows the grants of a player in a menu.",
        usage = "/grants <player>"
)
public final class GrantsCommand implements ICommand {

    private final ServiceRegistry registry;
    private final Lord plugin;
    private final MenuManager menuManager;
    private final GrantCacheService grantCacheService;

    public GrantsCommand(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.menuManager = registry.get(MenuManager.class);
        this.grantCacheService = registry.get(GrantCacheService.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return;
        }

        if (context.length() != 1) {
            player.sendMessage(Component.text("Usage: /grants <player>", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        player.sendMessage(Component.text("Searching for player '" + targetName + "'...", NamedTextColor.YELLOW));

        // 1. Asenkron olarak oyuncunun UUID'sini bul.
        PlayerResolver.resolveUUID(targetName).thenAcceptAsync(targetUuidOpt -> {
            if (targetUuidOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(Component.text("Player not found.", NamedTextColor.RED)));
                return;
            }

            UUID targetUuid = targetUuidOpt.get();

            // 2. Bulunan UUID ile grant'ları önbellekten (veya gerekirse DB'den) asenkron olarak çek.
            this.grantCacheService.getGrants(targetUuid).thenAccept(grants -> {
                // 3. Grant'lar geldiğinde, ana thread'e dönerek menüyü hazır verilerle aç.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    this.menuManager.open(player, new GrantsMenu(targetUuid, targetName, grants, this.registry));
                });
            });
        });
    }
}
