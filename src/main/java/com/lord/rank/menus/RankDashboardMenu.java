package com.lord.rank.menus;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.menus.wizards.RankCreationWizard;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class RankDashboardMenu extends MenuView {

    private final ServiceRegistry registry;

    public RankDashboardMenu(ServiceRegistry registry) {
        super("Rank Management", 3);
        this.registry = registry;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();

        UIComponent createRankButton = new ButtonBuilder(Material.ANVIL)
                .name("<green>Create a New Rank")
                .lore(
                        "<gray>Opens the rank creation wizard to",
                        "<gray>create a new rank from scratch."
                )
                .onClick(event -> {
                    // Butona tıklandığında, yeni sihirbazı oluştur ve başlat!
                    new RankCreationWizard(this.registry, viewer).start();
                })
                .build();

        UIComponent editRanksButton = new ButtonBuilder(Material.BOOKSHELF)
                .name("<aqua>Edit Existing Ranks")
                .lore(
                        "<gray>Opens a list of all existing ranks",
                        "<gray>to view or edit them."
                )
                .onClick(event -> {
                    viewer.closeInventory();
                    viewer.sendMessage(Component.text("Rank List & Editor will open here...", NamedTextColor.YELLOW));
                })
                .build();

        components.put(12, createRankButton);
        components.put(14, editRanksButton);

        return components;
    }
}
