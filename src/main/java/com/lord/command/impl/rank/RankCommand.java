package com.lord.command.impl.rank;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.data.ranks.Rank;
import com.lord.repositories.RankRepository;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

@Command(
        name = "rank",
        permission = "lord.command.rank",
        description = "Commands for rank management.",
        usage = "/rank <create|list|delete|setprefix|...>"
)
public final class RankCommand implements ICommand {

    private final RankRepository rankRepository;

    public RankCommand(ServiceRegistry registry) {
        this.rankRepository = registry.get(RankRepository.class);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.length() == 0) {
            context.sender().sendMessage(Component.text("Usage: /rank <create|list|delete|...>", NamedTextColor.RED));
            return;
        }

        String subCommand = context.arg(0).toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(context);
            case "list" -> handleList(context);
            case "delete" -> handleDelete(context);
            case "setprefix" -> handleSetPrefix(context);
            case "setsuffix" -> handleSetSuffix(context);
            case "setpriority" -> handleSetPriority(context);
            default -> context.sender().sendMessage(Component.text("Unknown sub-command: " + subCommand, NamedTextColor.RED));
        }
    }

    private void handleCreate(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.create")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (context.length() < 3) {
            sender.sendMessage(Component.text("Usage: /rank create <name> <priority>", NamedTextColor.RED));
            return;
        }

        String rankName = context.arg(1);
        String priorityArg = context.arg(2);
        int priority;

        try {
            priority = Integer.parseInt(priorityArg);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Priority must be a number.", NamedTextColor.RED));
            return;
        }

        if (this.rankRepository.findByName(rankName).isPresent()) {
            sender.sendMessage(Component.text("A rank with this name already exists.", NamedTextColor.RED));
            return;
        }

        Rank newRank = new Rank(rankName);
        newRank.setPriority(priority);

        this.rankRepository.save(newRank);

        sender.sendMessage(Component.text("The rank " + rankName + " was created successfully with priority " + priority + ".", NamedTextColor.GREEN));
    }

    private void handleList(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.list")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        Set<Rank> ranks = this.rankRepository.getAllRanks();

        if (ranks.isEmpty()) {
            sender.sendMessage(Component.text("No ranks found in the system.", NamedTextColor.YELLOW));
            return;
        }

        List<Rank> sortedRanks = new ArrayList<>(ranks);
        sortedRanks.sort(Comparator.comparingInt(Rank::getPriority).reversed());

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>» <gold>Ranks (<aqua><count></aqua>)</gold> <dark_gray>«</dark_gray>",
                Placeholder.unparsed("count", String.valueOf(sortedRanks.size()))));

        for (Rank rank : sortedRanks) {
            Component rankComponent = MiniMessage.miniMessage().deserialize(
                    "<gray> - <white><name></white> (<yellow>Priority: <gold><priority></gold></yellow>) | <white>Prefix: <reset><prefix>",
                    Placeholder.unparsed("name", rank.getName()),
                    Placeholder.unparsed("priority", String.valueOf(rank.getPriority())),
                    Placeholder.component("prefix", Component.text(rank.getPrefix() != null ? rank.getPrefix() : "None"))
            );
            sender.sendMessage(rankComponent);
        }
    }

    private void handleDelete(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.delete")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (context.length() < 2) {
            sender.sendMessage(Component.text("Usage: /rank delete <name>", NamedTextColor.RED));
            return;
        }

        String rankName = context.arg(1);

        if (rankName.equalsIgnoreCase("default")) {
            sender.sendMessage(Component.text("The 'default' rank cannot be deleted.", NamedTextColor.RED));
            return;
        }

        if (this.rankRepository.findByName(rankName).isEmpty()) {
            sender.sendMessage(Component.text("A rank with this name was not found.", NamedTextColor.RED));
            return;
        }

        this.rankRepository.delete(rankName);
        sender.sendMessage(Component.text("The rank " + rankName + " was successfully deleted.", NamedTextColor.GREEN));
    }

    private void handleSetPrefix(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.setprefix")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (context.length() < 3) {
            sender.sendMessage(Component.text("Usage: /rank setprefix <name> <prefix>", NamedTextColor.RED));
            return;
        }

        String rankName = context.arg(1);

        this.rankRepository.findByName(rankName).ifPresentOrElse(rank -> {
            String newPrefix = Arrays.stream(context.args(), 2, context.args().length)
                    .collect(Collectors.joining(" "));

            rank.setPrefix(newPrefix);
            this.rankRepository.save(rank);

            Component successMessage = MiniMessage.miniMessage().deserialize(
                    "<green>Set new prefix for rank <white><rank_name></white> to: '<reset><prefix><green>'",
                    Placeholder.unparsed("rank_name", rank.getName()),
                    Placeholder.component("prefix", MiniMessage.miniMessage().deserialize(newPrefix))
            );
            sender.sendMessage(successMessage);

        }, () -> sender.sendMessage(Component.text("A rank with this name was not found.", NamedTextColor.RED)));
    }

    private void handleSetSuffix(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.setsuffix")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (context.length() < 3) {
            sender.sendMessage(Component.text("Usage: /rank setsuffix <name> <suffix>", NamedTextColor.RED));
            return;
        }

        String rankName = context.arg(1);

        this.rankRepository.findByName(rankName).ifPresentOrElse(rank -> {
            String newSuffix = Arrays.stream(context.args(), 2, context.args().length)
                    .collect(Collectors.joining(" "));

            rank.setSuffix(newSuffix);
            this.rankRepository.save(rank);

            Component successMessage = MiniMessage.miniMessage().deserialize(
                    "<green>Set new suffix for rank <white><rank_name></white> to: '<reset><suffix><green>'",
                    Placeholder.unparsed("rank_name", rank.getName()),
                    Placeholder.component("suffix", MiniMessage.miniMessage().deserialize(newSuffix))
            );
            sender.sendMessage(successMessage);

        }, () -> sender.sendMessage(Component.text("A rank with this name was not found.", NamedTextColor.RED)));
    }

    private void handleSetPriority(CommandContext context) {
        CommandSender sender = context.sender();

        if (!sender.hasPermission("lord.command.rank.setpriority")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (context.length() != 3) {
            sender.sendMessage(Component.text("Usage: /rank setpriority <name> <priority>", NamedTextColor.RED));
            return;
        }

        String rankName = context.arg(1);
        String priorityArg = context.arg(2);
        int newPriority;

        try {
            newPriority = Integer.parseInt(priorityArg);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Priority must be a number.", NamedTextColor.RED));
            return;
        }

        this.rankRepository.findByName(rankName).ifPresentOrElse(rank -> {
            rank.setPriority(newPriority);
            this.rankRepository.save(rank);

            sender.sendMessage(Component.text("Set new priority for rank " + rank.getName() + " to " + newPriority + ".", NamedTextColor.GREEN));

        }, () -> sender.sendMessage(Component.text("A rank with this name was not found.", NamedTextColor.RED)));
    }
}