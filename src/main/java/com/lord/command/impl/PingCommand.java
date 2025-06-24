package com.lord.command.impl;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(
        name = "ping",
        permission = "titan.command.ping",
        description = "Kendi ping'inizi veya başka bir oyuncunun ping'ini gösterir.",
        usage = "/ping [oyuncu]"
)
public class PingCommand implements ICommand {

    @Override
    public void execute(CommandContext context) {
        String[] args = context.args();
        CommandSender sender = context.sender();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Konsolun pingi yoktur. Bir oyuncu belirtin: /ping <oyuncu>"
                ));
                return;
            }

            int ping = player.getPing();
            Component msg = MiniMessage.miniMessage().deserialize(
                    "<green>Pinginiz: <yellow><ping>ms",
                    Placeholder.unparsed("ping", String.valueOf(ping))
            );
            player.sendMessage(msg);
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Oyuncu bulunamadı: <gray><target>",
                        Placeholder.unparsed("target", args[0])
                ));
                return;
            }

            int ping = target.getPing();
            Component msg = MiniMessage.miniMessage().deserialize(
                    "<green><target> adlı oyuncunun pingi: <yellow><ping>ms",
                    Placeholder.unparsed("target", target.getName()),
                    Placeholder.unparsed("ping", String.valueOf(ping))
            );
            sender.sendMessage(msg);
        }
    }
}