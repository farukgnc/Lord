package com.lord.rank.menus.editor;

import com.lord.menu.AbstractPaginatedMenu;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RankListMenu extends AbstractPaginatedMenu<Rank> {

    public RankListMenu(ServiceRegistry registry) {
        super(registry, "Edit a Rank", 6);
    }

    @Override
    public List<Rank> getElements() {
        // Listelenecek tüm öğeleri RankRepository'den alıp sıralıyoruz.
        RankRepository rankRepository = this.registry.get(RankRepository.class);
        List<Rank> ranks = new ArrayList<>(rankRepository.getAllRanks());
        ranks.sort(Comparator.comparingInt(Rank::getPriority).reversed());
        return ranks;
    }

    @Override
    public UIComponent convertElement(Rank rank) {
        // Tek bir Rank nesnesini alıp, onu tıklanabilir bir butona çeviriyoruz.
        return new ButtonBuilder(Material.NAME_TAG)
                .name("<green>" + rank.getName())
                .lore(
                        "<gray>Priority: <yellow>" + rank.getPriority(),
                        "<gray>Prefix: " + (rank.getPrefix() != null ? "<white>" + rank.getPrefix() : "<i>None"),
                        "",
                        "<yellow>Click to edit this rank."
                )
                .onClick(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    this.getMenuManager().open(viewer, new RankEditorMenu(rank, this.registry));
                }).build();
    }
}
