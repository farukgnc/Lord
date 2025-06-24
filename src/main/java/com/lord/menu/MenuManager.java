package com.lord.menu;

import com.lord.Lord;
import com.lord.menu.buttons.MenuButton;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuManager implements Listener {

    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();

    public MenuManager(Lord plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player, Menu menu) {
        int size = menu.getRows() * 9;
        Inventory inventory = Bukkit.createInventory(
                player,
                size,
                MiniMessage.miniMessage().deserialize(menu.getTitle())
        );

        Map<Integer, MenuButton> buttons = menu.getButtons(player);
        for (Map.Entry<Integer, MenuButton> entry : buttons.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItem());
        }

        this.openMenus.put(player.getUniqueId(), menu);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Menu openMenu = this.openMenus.get(player.getUniqueId());

        if (openMenu == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getOpenInventory().getBottomInventory())) {
            return;
        }

        Map<Integer, MenuButton> buttons = openMenu.getButtons(player);
        MenuButton clickedButton = buttons.get(event.getSlot());

        if (clickedButton != null) {
            clickedButton.getClickAction().accept(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        this.openMenus.remove(player.getUniqueId());
    }
}