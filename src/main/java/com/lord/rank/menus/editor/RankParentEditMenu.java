package com.lord.rank.menus.editor;

import com.lord.Lord;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.RankService;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public final class RankParentEditMenu extends MenuView {

    private final Rank targetRank;
    private final ServiceRegistry registry;
    private final Lord plugin;
    private final RankService rankService;
    // Bu menü açıkken yapılan seçimleri geçici olarak saklamak için.
    private final Set<String> selectedParents;

    public RankParentEditMenu(Rank targetRank, ServiceRegistry registry) {
        super("Manage Parents: " + targetRank.getName(), 6);
        this.targetRank = targetRank;
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.rankService = registry.get(RankService.class);
        // Menü açıldığında, rütbenin mevcut ebeveynleriyle geçici listeyi doldur.
        this.selectedParents = new HashSet<>(targetRank.getParentRankNames());
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();
        RankRepository rankRepository = this.registry.get(RankRepository.class);

        List<Rank> availableRanks = new ArrayList<>(rankRepository.getAllRanks());
        availableRanks.removeIf(r -> r.getName().equalsIgnoreCase(this.targetRank.getName()));
        availableRanks.sort(Comparator.comparingInt(Rank::getPriority).reversed());

        int slot = 0;
        for (Rank rank : availableRanks) {
            if(slot >= 45) break;
            components.put(slot++, createRankButton(rank));
        }

        UIComponent saveButton = new ButtonBuilder(Material.GREEN_DYE)
                .name("<green>Save & Go Back")
                .lore("<gray>Saves the changes and returns to the editor.")
                .onClick(event -> {
                    // Değişiklikleri asıl Rank nesnesine uygula
                    this.targetRank.getParentRankNames().clear();
                    this.targetRank.getParentRankNames().addAll(this.selectedParents);

                    this.rankService.saveRank(this.targetRank, player).whenComplete((success, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (throwable == null && Boolean.TRUE.equals(success)) {
                            player.sendMessage(Component.text("Parents for rank '" + targetRank.getName() + "' updated.", NamedTextColor.GREEN));
                        }

                        // Düzenleme menüsüne geri dön
                        this.getMenuManager().open(player, new RankEditorMenu(this.targetRank, this.registry));
                    }));
                })
                .build();

        components.put(53, saveButton);

        return components;
    }

    private UIComponent createRankButton(Rank rank) {
        boolean isSelected = this.selectedParents.contains(rank.getName());

        ButtonBuilder builder = new ButtonBuilder(isSelected ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name((isSelected ? "<green>" : "<white>") + rank.getName())
                .lore("<yellow>Click to toggle selection.")
                .onClick(event -> {
                    if (isSelected) {
                        this.selectedParents.remove(rank.getName());
                    } else {
                        this.selectedParents.add(rank.getName());
                    }
                    this.refresh();
                });

        if (isSelected) {
            builder.enchant();
        }

        return builder.build();
    }
}
