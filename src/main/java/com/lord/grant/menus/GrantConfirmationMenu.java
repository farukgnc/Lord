package com.lord.grant.menus;

import com.lord.grant.GrantService;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GrantConfirmationMenu extends MenuView {

    private final GrantWizard wizard;

    public GrantConfirmationMenu(GrantWizard wizard) {
        super("Confirm Grant", 3);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Onay Butonu
        components.put(11, new ButtonBuilder(Material.GREEN_WOOL)
                .name("<green>Confirm Grant")
                .lore(
                        "<gray>Click to confirm and grant the rank."
                )
                .onClick(event -> {
                    GrantService grantService = wizard.getRegistry().get(GrantService.class);
                    
                    grantService.createGrant(
                        wizard.getTargetUuid(),
                        wizard.getTargetName(),
                        wizard.getSelectedRank().getName(),
                        wizard.getIssuer(),
                        wizard.getSelectedDuration()
                    ).thenAccept(grant -> {
                        player.sendMessage(Component.text("Grant created successfully!", NamedTextColor.GREEN));
                    }).exceptionally(throwable -> {
                        player.sendMessage(Component.text("Failed to create grant: " + throwable.getMessage(), NamedTextColor.RED));
                        return null;
                    });

                    player.closeInventory();
                })
                .build());

        // Bilgi Paneli
        String durationString = wizard.getSelectedDuration().isZero() ? "Permanent" : TimeUtil.formatDuration(wizard.getSelectedDuration());
        components.put(13, new ButtonBuilder(Material.PAPER)
                .name("<yellow>Grant Summary")
                .lore(
                        "<gray>Target: <white>" + wizard.getTargetName(),
                        "<gray>Rank: <white>" + wizard.getSelectedRank().getName(),
                        "<gray>Duration: <white>" + durationString
                )
                .build());

        // İptal Butonu
        components.put(15, new ButtonBuilder(Material.RED_WOOL)
                .name("<red>Cancel")
                .lore(
                        "<gray>Click to cancel the process."
                )
                .onClick(event -> player.closeInventory())
                .build());

        return components;
    }
}
