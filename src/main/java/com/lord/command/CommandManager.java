package com.lord.command;

import com.lord.Lord;
import com.lord.command.annotations.Command;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Method;
import java.util.List;

public class CommandManager {

    private final Lord plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CommandManager(Lord plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(Object instance) {
        Class<?> clazz = instance.getClass();

        if (!clazz.isAnnotationPresent(Command.class)) return;

        Command meta = clazz.getAnnotation(Command.class);
        String name = meta.name();

        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            try {
                SimpleCommandMap commandMap = getCommandMap();
                DynamicCommand dynCmd = new DynamicCommand(meta, instance);
                commandMap.register(plugin.getName(), dynCmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private SimpleCommandMap getCommandMap() throws Exception {
        Method getCommandMap = Bukkit.getServer().getClass().getMethod("getCommandMap");
        return (SimpleCommandMap) getCommandMap.invoke(Bukkit.getServer());
    }

    private class DynamicCommand extends org.bukkit.command.Command {
        private final Object commandInstance;
        private final Command meta;

        protected DynamicCommand(Command meta, Object instance) {
            super(meta.name(), meta.description(), meta.usage(), List.of(meta.aliases()));
            this.commandInstance = instance;
            this.meta = meta;
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!meta.permission().isEmpty() && !sender.hasPermission(meta.permission())) {
                sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to use this command.</red>"));
                return true;
            }

            try {
                if (commandInstance instanceof ICommand cmd) {
                    CommandContext context = new CommandContext(sender, args);
                    cmd.execute(context);
                    return true;
                }

                sender.sendMessage(miniMessage.deserialize("<red>Command class does not implement the <gray>ICommand</gray> interface.</red>"));
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(miniMessage.deserialize("<red>An error occurred while executing the command.</red>"));
            }

            return true;
        }
    }
}