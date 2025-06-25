package com.lord.rank.menus.creation;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public final class ParentSelectionMenu extends MenuView {

    private final RankCreationWizard wizard;

    // Constructor artık Rank yerine RankCreationWizard alıyor.
    public ParentSelectionMenu(RankCreationWizard wizard) {
        super("Select Parent(s) for '" + wizard.getName() + "'", 6);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();
        RankRepository rankRepository = wizard.getRegistry().get(RankRepository.class);

        // Olası ebeveynleri listele
        List<Rank> availableRanks = new ArrayList<>(rankRepository.getAllRanks());
        availableRanks.sort(Comparator.comparingInt(Rank::getPriority).reversed());

        int slot = 0;
        for (Rank rank : availableRanks) {
            if (slot >= 45) break;
            components.put(slot++, createRankButton(rank));
        }

        // Bitti Butonu
        UIComponent doneButton = new ButtonBuilder(Material.GREEN_DYE)
                .name("<green>Done")
                .lore("<gray>Click to confirm parent selection.")
                .onClick(event -> {
                    // Tıklandığında, sihirbazın bir sonraki adımına geç.
                    wizard.advanceToConfirmation();
                })
                .build();

        components.put(53, doneButton);

        return components;
    }

    private UIComponent createRankButton(Rank rank) {
        // Rütbenin seçili olup olmadığını wizard'ın içindeki geçici listeden kontrol et.
        boolean isSelected = wizard.getSelectedParentNames().contains(rank.getName());

        ButtonBuilder builder = new ButtonBuilder(isSelected ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name((isSelected ? "<green>" : "<white>") + rank.getName())
                .lore("<yellow>Click to toggle selection.")
                .onClick(event -> {
                    // Seçimi wizard'ın içindeki geçici listede güncelle.
                    if (isSelected) {
                        wizard.getSelectedParentNames().remove(rank.getName());
                    } else {
                        wizard.getSelectedParentNames().add(rank.getName());
                    }
                    this.refresh(); // Menüyü anında yenile.
                });

        if (isSelected) {
            builder.enchant();
        }

        return builder.build();
    }
}
