package com.lord.rank.commands;

import com.lord.command.CommandContext;
import com.lord.command.ICommand;
import com.lord.command.annotations.Command;
import com.lord.menu.MenuManager;
import com.lord.rank.menus.RankDashboardMenu;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(
        name = "rank",
        permission = "lord.command.rank",
        description = "Opens the rank management menu.", // Açıklama güncellendi
        usage = "/rank" // Kullanım güncellendi
)
public final class RankCommand implements ICommand {

    private final ServiceRegistry registry;
    private final MenuManager menuManager;

    public RankCommand(ServiceRegistry registry) {
        this.registry = registry;
        this.menuManager = registry.get(MenuManager.class);
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }

        // Komutun tek görevi: Ana yönetim menüsünü açmak.
        this.menuManager.open(player, new RankDashboardMenu(this.registry));
    }
}
