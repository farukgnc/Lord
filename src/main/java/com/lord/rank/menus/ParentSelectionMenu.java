package com.lord.rank.menus;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.menus.wizards.RankCreationWizard;
import com.lord.rank.repositories.RankRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class ParentSelectionMenu extends MenuView {

    private final RankCreationWizard wizard;

    public ParentSelectionMenu(RankCreationWizard wizard) {
        super("Select Parent(s) for '" + wizard.getName() + "'", 6);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();
        RankRepository rankRepository = wizard.getRegistry().get(RankRepository.class);

        List<Rank> availableRanks = rankRepository.getAllRanks().stream()
                .filter(rank -> !rank.getName().equalsIgnoreCase(wizard.getName()))
                .filter(rank -> !rank.getName().equalsIgnoreCase("default"))
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .collect(Collectors.toList());

        int slot = 0;
        for (Rank rank : availableRanks) {
            if (slot > 44) break; // Sayfalama için alt sırayı boş bırak
            components.put(slot++, createRankButton(rank));
        }

        // Onaylama Butonu
        UIComponent doneButton = new ButtonBuilder(Material.GREEN_DYE)
                .name("<green>Done")
                .lore(
                        "<gray>Click to confirm parent selection",
                        "<gray>and move to the confirmation step."
                )
                .onClick(event -> {
                    // Sihirbazın son onay adımına geçmesini sağlıyoruz.
                    wizard.advanceToConfirmation();
                })
                .build();

        components.put(53, doneButton); // Sağ alt köşe

        return components;
    }

    private UIComponent createRankButton(Rank rank) {
        boolean isSelected = wizard.getSelectedParentNames().contains(rank.getName());

        ButtonBuilder builder = new ButtonBuilder(isSelected ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name((isSelected ? "<green>" : "<white>") + rank.getName())
                .lore(
                        "<gray>Priority: <yellow>" + rank.getPriority(),
                        "",
                        isSelected ? "<red>Click to deselect." : "<yellow>Click to select as parent."
                )
                .onClick(event -> {
                    if (isSelected) {
                        wizard.getSelectedParentNames().remove(rank.getName());
                    } else {
                        wizard.getSelectedParentNames().add(rank.getName());
                    }
                    this.refresh();
                });

        if (isSelected) {
            builder.enchant();
        }

        return builder.build();
    }
}
