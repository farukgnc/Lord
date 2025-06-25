package com.lord.menu;

import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ConfirmationMenu extends MenuView {

    private final String confirmationText;
    private final Consumer<InventoryClickEvent> onConfirm;
    private final Consumer<InventoryClickEvent> onCancel; // onCancel eylemi eklendi.

    // Constructor artık onCancel eylemini de alıyor.
    public ConfirmationMenu(String title, String confirmationText, Consumer<InventoryClickEvent> onConfirm, Consumer<InventoryClickEvent> onCancel) {
        super(title, 3);
        this.confirmationText = confirmationText;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Onayla Butonu
        components.put(11, new ButtonBuilder(Material.GREEN_WOOL)
                .name("<green><bold>CONFIRM")
                .lore("<gray>Click to confirm this action.")
                .onClick(this.onConfirm) // Dışarıdan gelen onConfirm eylemini kullanır.
                .build());

        // Bilgi Paneli
        components.put(13, new ButtonBuilder(Material.PAPER)
                .name("<yellow><bold>Are you sure?")
                .lore(this.confirmationText.split("\n"))
                .build());

        // İptal Butonu
        components.put(15, new ButtonBuilder(Material.RED_WOOL)
                .name("<red><bold>CANCEL")
                .lore("<gray>Click to cancel and go back.")
                .onClick(this.onCancel) // Artık sabit değil, dışarıdan gelen onCancel eylemini kullanır.
                .build());

        return components;
    }
}
