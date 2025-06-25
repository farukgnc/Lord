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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuManager implements Listener {

    private final Lord plugin;
    private final Map<UUID, MenuView> openMenus = new ConcurrentHashMap<>();

    // Menüler arası geçiş yapan oyuncuları takip eden, thread-safe olmayan basit bir Set.
    // Çünkü tüm envanter olayları ana thread üzerinde çalışır, bu yüzden senkronizasyona gerek yoktur.
    private final Set<UUID> playersInTransition = new HashSet<>();

    public MenuManager(Lord plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Bir oyuncuya yeni bir menü açar.
     */
    public void open(Player player, MenuView menu) {
        menu.setViewer(player);
        menu.setMenuManager(this);

        Inventory inventory = Bukkit.createInventory(
                player,
                menu.getRows() * 9,
                MiniMessage.miniMessage().deserialize(menu.getTitle())
        );

        render(player, menu, inventory);

        // Oyuncuyu "geçişte" olarak işaretle.
        this.playersInTransition.add(player.getUniqueId());

        this.openMenus.put(player.getUniqueId(), menu);
        player.openInventory(inventory);

        // Bukkit'in bir sonraki tick'inde (oyun döngüsünde) geçiş işaretini kaldır.
        // Bu, InventoryCloseEvent'in doğru çalışmasını sağlar.
        Bukkit.getScheduler().runTask(this.plugin, () -> this.playersInTransition.remove(player.getUniqueId()));
    }

    /**
     * Bir oyuncunun açık olan menüsünün içeriğini, menüyü kapatmadan günceller.
     */
    public void update(Player player) {
        MenuView openMenu = this.openMenus.get(player.getUniqueId());
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (openMenu != null && topInventory != null) {
            render(player, openMenu, topInventory);
            player.updateInventory();
        }
    }

    /**
     * Bir menünün içeriğini oluşturur ve verilen envantere çizer.
     */
    private void render(Player player, MenuView menu, Inventory inventory) {
        inventory.clear();
        menu.build(player); // Menünün bileşen haritasını oluşturup önbelleğe alır.
        for (Map.Entry<Integer, UIComponent> entry : menu.getComposedComponents().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().render(player));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        MenuView openMenu = this.openMenus.get(player.getUniqueId());

        if (openMenu == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
            return;
        }

        UIComponent clickedComponent = openMenu.getComposedComponents().get(event.getSlot());

        if (clickedComponent != null) {
            clickedComponent.getAction().accept(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        // Eğer oyuncu bir menüden diğerine geçiş yapıyorsa (bayrak kaldırılmışsa),
        // kaydını silme. Bu, yarış durumunu engeller.
        if (this.playersInTransition.contains(player.getUniqueId())) {
            return;
        }

        this.openMenus.remove(player.getUniqueId());
    }
}
