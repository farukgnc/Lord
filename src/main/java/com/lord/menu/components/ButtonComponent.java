package com.lord.menu.components;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@AllArgsConstructor
public class ButtonComponent implements UIComponent {

    private ItemStack item;
    private Consumer<InventoryClickEvent> action;

    @Override
    public ItemStack render(Player player) {
        return this.item;
    }

    @Override
    public Consumer<InventoryClickEvent> getAction() {
        return this.action;
    }
}