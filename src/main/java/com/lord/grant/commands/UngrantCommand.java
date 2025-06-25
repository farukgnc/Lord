package com.lord.grant.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Command(
        name = "ungrant",
        permission = "lord.command.ungrant",
        description = "Revokes a specific grant from a player.",
        usage = "/ungrant <grant-id>"
)
public final class UngrantCommand implements ICommand {

    private final GrantRepository grantRepository;
    private final PlayerDataCache playerDataCache;

    public UngrantCommand(ServiceRegistry registry) {
        this.grantRepository = registry.get(GrantRepository.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        if (context.length() != 1) {
            sender.sendMessage(Component.text("Usage: /ungrant <grant-id>", NamedTextColor.RED));
            return;
        }

        String grantIdArg = context.arg(0);
        UUID grantId;

        try {
            grantId = UUID.fromString(grantIdArg);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid Grant ID format.", NamedTextColor.RED));
            return;
        }

        this.grantRepository.findById(grantId).ifPresentOrElse(grant -> {
            this.grantRepository.delete(grant);

            this.playerDataCache.invalidate(grant.getGranteeUuid());

            OfflinePlayer target = Bukkit.getOfflinePlayer(grant.getGranteeUuid());
            String targetName = target.getName() != null ? target.getName() : "an unknown player";

            sender.sendMessage(Component.text("Successfully revoked rank " + grant.getRankName() + " from " + targetName + ".", NamedTextColor.GREEN));

            if (target.isOnline()) {
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null) {
                    onlineTarget.sendMessage(Component.text("Your " + grant.getRankName() + " rank has been revoked.", NamedTextColor.YELLOW));
                }
            }

        }, () -> sender.sendMessage(Component.text("A grant with ID " + grantIdArg + " was not found.", NamedTextColor.RED)));
    }
}