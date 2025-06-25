package com.lord.menu;

import com.lord.Lord;
import com.lord.menu.components.UIComponent;
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

    private final Map<UUID, MenuView> openMenus = new ConcurrentHashMap<>();

    public MenuManager(Lord plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, MenuView menu) {
        // Menüye, onu kimin açtığını ve yöneticisinin kim olduğunu bildiriyoruz.
        menu.setViewer(player);
        menu.setMenuManager(this);

        int size = menu.getRows() * 9;
        Inventory inventory = Bukkit.createInventory(
                player,
                size,
                MiniMessage.miniMessage().deserialize(menu.getTitle())
        );

        render(player, menu, inventory);

        this.openMenus.put(player.getUniqueId(), menu);
        player.openInventory(inventory);
    }

    public void update(Player player) {
        MenuView openMenu = this.openMenus.get(player.getUniqueId());
        if (openMenu != null && player.getOpenInventory().getTopInventory() != null) {
            render(player, openMenu, player.getOpenInventory().getTopInventory());
            player.updateInventory();
        }
    }

    private void render(Player player, MenuView menu, Inventory inventory) {
        inventory.clear();
        Map<Integer, UIComponent> components = menu.compose(player);
        for (Map.Entry<Integer, UIComponent> entry : components.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().render(player));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        MenuView openMenu = this.openMenus.get(player.getUniqueId());

        if (openMenu == null || event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
            return;
        }

        event.setCancelled(true);

        UIComponent clickedComponent = openMenu.compose(player).get(event.getSlot());

        if (clickedComponent != null) {
            clickedComponent.getAction().accept(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.openMenus.remove(event.getPlayer().getUniqueId());
    }
}