package com.lord.menu.buttons;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public class MenuButton {

    private final ItemStack item;
    private final Consumer<InventoryClickEvent> clickAction;

}