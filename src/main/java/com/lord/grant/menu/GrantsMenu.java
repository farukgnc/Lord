package com.lord.grant.menu;

import com.lord.grant.Grant;
import com.lord.menu.Menu;
import com.lord.menu.MenuManager;
import com.lord.menu.buttons.MenuButton;
import com.lord.grant.repositories.GrantRepository;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class GrantsMenu extends Menu {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OfflinePlayer target;
    private final ServiceRegistry registry;

    public GrantsMenu(OfflinePlayer target, ServiceRegistry registry) {
        super("Grants: " + target.getName(), 4);
        this.target = target;
        this.registry = registry;
    }

    @Override
    public Map<Integer, MenuButton> getButtons(Player viewer) {
        Map<Integer, MenuButton> buttons = new HashMap<>();
        GrantRepository grantRepository = this.registry.get(GrantRepository.class);
        MenuManager menuManager = this.registry.get(MenuManager.class);
        Set<Grant> grants = grantRepository.findByPlayer(this.target.getUniqueId());

        if (grants.isEmpty()) {
            ItemStack noGrantsItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noGrantsItem.getItemMeta();
            meta.displayName(Component.text("No Grants Found", NamedTextColor.RED));
            noGrantsItem.setItemMeta(meta);

            buttons.put(22, new MenuButton(noGrantsItem, e -> {}));
            return buttons;
        }

        List<Grant> sortedGrants = new ArrayList<>(grants);
        sortedGrants.sort(Comparator.comparing(Grant::getCreationTime).reversed());

        int slot = 0;
        for (Grant grant : sortedGrants) {
            ItemStack item = new ItemStack(grant.isActive() ? Material.ENCHANTED_BOOK : Material.BOOK);
            ItemMeta meta = item.getItemMeta();

            Component statusComponent = grant.isActive() ?
                    Component.text("Active", NamedTextColor.GREEN) :
                    Component.text("Expired", NamedTextColor.RED);

            String issuerName;
            if (grant.getIssuerUuid() == null) {
                issuerName = "Console";
            } else {
                OfflinePlayer issuer = Bukkit.getOfflinePlayer(grant.getIssuerUuid());
                issuerName = issuer.getName() != null ? issuer.getName() : "Unknown";
            }

            meta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#5e4fa2:#f79459><rank_name></gradient>",
                    Placeholder.unparsed("rank_name", grant.getRankName())));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: <white>" + grant.getUniqueId().toString().substring(0, 8)));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Status: ").append(statusComponent));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Granted By: <white>" + issuerName));
            lore.add(Component.text(""));

            String durationString = grant.isPermanent() ? "Permanent" : TimeUtil.formatDuration(grant.getDuration());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Duration: <white>" + durationString));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Granted At: <white>" + DATE_FORMATTER.format(grant.getCreationTime())));
            if (!grant.isPermanent()) {
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Expires At: <white>" + DATE_FORMATTER.format(grant.getExpiry())));
            }
            lore.add(Component.text(""));
            lore.add(MiniMessage.miniMessage().deserialize("<red>Right-click to revoke this grant.")); // Kullanıcıyı bilgilendiriyoruz.


            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            // --- TIKLAMA EYLEMİ BURADA TANIMLANIYOR ---
            buttons.put(slot++, new MenuButton(item, e -> {
                // Sadece sağ tıklamaları dikkate al.
                if (!e.getClick().isRightClick()) {
                    return;
                }

                // Eylemi gerçekleştiren oyuncu (menüyü görüntüleyen)
                Player clicker = (Player) e.getWhoClicked();

                // Silme işlemini yap.
                grantRepository.delete(grant);

                // Geri bildirimde bulun.
                clicker.sendMessage(Component.text("Successfully revoked grant for rank " + grant.getRankName() + " from " + this.target.getName(), NamedTextColor.GREEN));

                // Menüyü yenilemek için tekrar aç.
                menuManager.openMenu(clicker, new GrantsMenu(this.target, this.registry));
            }));
        }

        return buttons;
    }
}