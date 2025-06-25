package com.lord.grant.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.grant.Grant;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(
        name = "grant",
        permission = "lord.command.grant",
        description = "Assigns a rank to a player.",
        usage = "/grant <player> <rank> [duration] [reason...]"
)
public final class GrantCommand implements ICommand {

    private final GrantRepository grantRepository;
    private final RankRepository rankRepository;
    private final PlayerDataCache playerDataCache;

    public GrantCommand(ServiceRegistry registry) {
        this.grantRepository = registry.get(GrantRepository.class);
        this.rankRepository = registry.get(RankRepository.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        if (context.length() < 2) {
            sender.sendMessage(Component.text("Usage: /grant <player> <rank> [duration] [reason...]", NamedTextColor.RED));
            return;
        }

        String targetName = context.arg(0);
        String rankName = context.arg(1);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }

        if (this.rankRepository.findByName(rankName).isEmpty()) {
            sender.sendMessage(Component.text("Rank not found: " + rankName, NamedTextColor.RED));
            return;
        }

        // Oyuncunun bu rütbeye ait aktif bir grant'i olup olmadığını kontrol et.
        Set<Grant> existingGrants = this.grantRepository.findByPlayer(target.getUniqueId());
        boolean hasActiveGrant = existingGrants.stream()
                .anyMatch(grant -> grant.getRankName().equalsIgnoreCase(rankName) && grant.isActive());

        if (hasActiveGrant) {
            sender.sendMessage(Component.text("Player " + target.getName() + " already has an active grant for the " + rankName + " rank.", NamedTextColor.RED));
            return;
        }

        Duration duration = TimeUtil.parseDuration(context.arg(2));
        String reason = context.length() > 3 ?
                Arrays.stream(context.args(), 3, context.args().length).collect(Collectors.joining(" ")) :
                "No reason specified.";

        UUID issuerUuid = (sender instanceof Player player) ? player.getUniqueId() : null;

        Grant newGrant = new Grant(target.getUniqueId(), rankName, issuerUuid, duration);
        this.grantRepository.save(newGrant);

        this.playerDataCache.invalidate(target.getUniqueId());

        sender.sendMessage(Component.text("Successfully granted rank " + rankName + " to " + target.getName() + ".", NamedTextColor.GREEN));

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.sendMessage(Component.text("You have been granted the " + rankName + " rank.", NamedTextColor.GREEN));
            }
        }
    }
}