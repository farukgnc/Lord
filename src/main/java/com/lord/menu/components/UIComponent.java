package com.lord.menu.components;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public interface UIComponent {

    /**
     * Bu bileşeni temsil eden ItemStack'i render eder.
     * @param player Menüyü görüntüleyen oyuncu.
     * @return Görsel olarak gösterilecek ItemStack.
     */
    ItemStack render(Player player);

    /**
     * Bu bileşene tıklandığında çalışacak olan eylemi tanımlar.
     * @return Tıklama eylemini içeren bir Consumer.
     */
    Consumer<InventoryClickEvent> getAction();

}