package com.lord.chat;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@Command(
        name = "chat",
        permission = "lord.command.chat",
        usage = "/chat <on|off|clear>"
)
public final class ChatCommand implements ICommand {

    private final ChatService chatService;

    public ChatCommand(ServiceRegistry registry) {
        this.chatService = registry.get(ChatService.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.length() == 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /chat <on|off|clear>"));
            return;
        }

        String subCommand = context.arg(0).toLowerCase();
        switch (subCommand) {
            case "on" -> handleOn(sender);
            case "off" -> handleOff(sender);
            case "clear" -> handleClear(sender);
            default -> sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /chat <on|off|clear>"));
        }
    }

    private void handleOn(CommandSender sender) {
        if (chatService.isChatEnabled()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Chat is already enabled."));
            return;
        }

        chatService.setChatEnabled(true);
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<green>Chat has been enabled by " + sender.getName() + "."));
    }

    private void handleOff(CommandSender sender) {
        if (!chatService.isChatEnabled()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Chat is already disabled."));
            return;
        }

        chatService.setChatEnabled(false);
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<red>Chat has been disabled by " + sender.getName() + "."));
    }

    private void handleClear(CommandSender sender) {
        for (int i = 0; i < 150; i++) {
            Bukkit.broadcast(Component.text(" "));
        }
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<green>Chat has been cleared by " + sender.getName() + "."));
    }
}