package com.lord.punishment.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.menu.MenuManager;
import com.lord.punishment.PunishmentCacheService;
import com.lord.punishment.menus.PunishmentsMenu;
import com.lord.services.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Command(
        name = "punishments",
        permission = "lord.command.punishments",
        aliases = {"history", "check"},
        usage = "/punishments <player>"
)
public class PunishmentsCommand implements ICommand {

    private final ServiceRegistry registry;
    private final PunishmentCacheService punishmentCacheService;
    private final Lord plugin;
    private final MenuManager menuManager;

    public PunishmentsCommand(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.punishmentCacheService = registry.get(PunishmentCacheService.class);
        this.menuManager = registry.get(MenuManager.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return;
        }

        if (context.length() != 1) {
            player.sendMessage(Component.text("Usage: /punishments <player>", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        player.sendMessage(Component.text("Searching for player '" + targetName + "'...", NamedTextColor.YELLOW));

        PlayerResolver.resolveUUID(targetName).thenAcceptAsync(targetUuidOpt -> {
            if (targetUuidOpt.isEmpty()) {
                runOnMainThread(() -> player.sendMessage(Component.text("Player not found.", NamedTextColor.RED)));
                return;
            }
            UUID targetUuid = targetUuidOpt.get();

            // Akıllı önbellekten ceza geçmişini iste. Gerisini o halleder.
            this.punishmentCacheService.getPunishments(targetUuid).thenAccept(punishments -> {
                // Ana thread'e dönerek menüyü aç.
                runOnMainThread(() -> this.menuManager.open(player, new PunishmentsMenu(targetUuid, targetName, punishments, this.registry)));
            });
        });
    }

    private void runOnMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(this.plugin, task);
    }
}