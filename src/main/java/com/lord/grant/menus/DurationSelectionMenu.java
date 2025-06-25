package com.lord.grant.menus;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class DurationSelectionMenu extends MenuView {

    private final GrantWizard wizard;

    public DurationSelectionMenu(GrantWizard wizard) {
        super("Select Grant Duration", 3);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Hazır süre butonları
        components.put(10, createDurationButton(Duration.ofDays(1), "1 Day", Material.CLOCK));
        components.put(11, createDurationButton(Duration.ofDays(7), "7 Days", Material.CLOCK));
        components.put(12, createDurationButton(Duration.ofDays(30), "30 Days", Material.CLOCK));
        components.put(14, createDurationButton(Duration.ZERO, "Permanent", Material.NETHER_STAR));
        // TODO: AnvilInputMenu eklendiğinde "Custom Duration" butonu eklenebilir.

        return components;
    }

    private UIComponent createDurationButton(Duration duration, String name, Material material) {
        return new ButtonBuilder(material)
                .name("<green>" + name)
                .lore("<yellow>Click to select this duration.")
                .onClick(event -> {
                    wizard.setSelectedDuration(duration);
                    wizard.advanceToConfirmation();
                })
                .build();
    }
}
