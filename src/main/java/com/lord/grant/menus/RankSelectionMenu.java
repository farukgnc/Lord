package com.lord.grant.menus;

import com.lord.grant.menus.wizards.GrantWizard;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RankSelectionMenu extends MenuView {

    private final GrantWizard wizard;

    public RankSelectionMenu(GrantWizard wizard) {
        super("Select a Rank to Grant", 4);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();
        RankRepository rankRepository = wizard.getRegistry().get(RankRepository.class);

        List<Rank> ranks = rankRepository.getAllRanks().stream()
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .toList();

        int slot = 0;
        for (Rank rank : ranks) {
            components.put(slot++, new ButtonBuilder(Material.NAME_TAG)
                    .name("<green>" + rank.getName())
                    .lore(
                            "<gray>Priority: <yellow>" + rank.getPriority(),
                            "",
                            "<yellow>Click to select this rank."
                    )
                    .onClick(event -> {
                        // Sihirbazın durumunu güncelle ve bir sonraki adıma geç!
                        wizard.setSelectedRank(rank);
                        wizard.advanceToDurationSelection();
                    }).build()
            );
        }
        return components;
    }
}
