package com.lord.grant.commands;

import com.lord.Lord;
import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.grant.menus.GrantWizard;
import com.lord.services.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Command(
        name = "grant",
        permission = "lord.command.grant",
        description = "Assigns a rank to a player.",
        usage = "/grant <player>"
)
public final class GrantCommand implements ICommand {

    private final ServiceRegistry registry;
    private final Lord plugin;

    public GrantCommand(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
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
        player.sendMessage(Component.text("Searching for player '" + targetName + "'...", NamedTextColor.YELLOW));

        PlayerResolver.resolve(targetName).thenAcceptAsync(targetUuidOpt -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (targetUuidOpt.isEmpty()) {
                    sender.sendMessage(Component.text("A player with the name '" + targetName + "' could not be found.", NamedTextColor.RED));
                    return;
                }

                UUID targetUuid = targetUuidOpt.get();

                // Artık OfflinePlayer nesnesi oluşturmuyoruz.
                // GrantWizard'ın da UUID ve isim alacak şekilde güncellenmesi gerekir.
                new GrantWizard(this.registry, player, targetUuid, targetName).start();
            });
        });
    }
}