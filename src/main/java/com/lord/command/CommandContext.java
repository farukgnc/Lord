package com.lord.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandContext {

    private final CommandSender sender;
    private final String[] args;

    public CommandContext(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player player() {
        return isPlayer() ? (Player) sender : null;
    }

    public CommandSender sender() {
        return sender;
    }

    public String[] args() {
        return args;
    }

    public String arg(int index) {
        return index < args.length ? args[index] : null;
    }

    public int length() {
        return args.length;
    }
}