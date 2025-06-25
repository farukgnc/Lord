package com.lord.rank.menus.creation;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RankConfirmationMenu extends MenuView {

    private final RankCreationWizard wizard;

    public RankConfirmationMenu(RankCreationWizard wizard) {
        super("Confirm Rank Creation", 4);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Onay Butonu
        UIComponent confirmButton = new ButtonBuilder(Material.GREEN_WOOL)
                .name("<green>Confirm & Create Rank")
                .lore("<gray>Click to create the rank with the specified properties.")
                .onClick(event -> {
                    // Sihirbazdaki son create metodunu çağır ve menüyü kapat.
                    wizard.createRank();
                    player.closeInventory();
                })
                .build();

        // İptal Butonu
        UIComponent cancelButton = new ButtonBuilder(Material.RED_WOOL)
                .name("<red>Cancel")
                .lore("<gray>Click to cancel the creation process.")
                .onClick(event -> {
                    player.closeInventory();
                    player.sendMessage("Rank creation cancelled.");
                })
                .build();

        // Özet Bilgi Paneli
        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        summaryLore.add("<gray>Name: <white>" + wizard.getName());
        summaryLore.add("<gray>Priority: <yellow>" + wizard.getPriority());
        summaryLore.add("<gray>Prefix: " + (wizard.getPrefix() != null ? "<white>" + wizard.getPrefix() : "<italic>None"));
        summaryLore.add("<gray>Suffix: " + (wizard.getSuffix() != null ? "<white>" + wizard.getSuffix() : "<italic>None"));
        summaryLore.add("");
        summaryLore.add("<gray>Parents (" + wizard.getSelectedParentNames().size() + "):");
        wizard.getSelectedParentNames().forEach(parent -> summaryLore.add("<gray> - <white>" + parent));
        if (wizard.getSelectedParentNames().isEmpty()) {
            summaryLore.add("<gray> - <italic>None");
        }

        UIComponent summaryItem = new ButtonBuilder(Material.PAPER)
                .name("<gold>Creation Summary")
                .lore(summaryLore.toArray(new String[0]))
                .build();


        components.put(13, summaryItem);
        components.put(29, confirmButton);
        components.put(33, cancelButton);

        return components;
    }
}
