package com.lord.grant.menu;

import com.lord.grant.Grant;
import com.lord.grant.repositories.GrantRepository;
import com.lord.menu.MenuManager;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class GrantsMenu extends MenuView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OfflinePlayer target;
    private final ServiceRegistry registry;

    public GrantsMenu(OfflinePlayer target, ServiceRegistry registry) {
        super("Grants: " + target.getName(), 6);
        this.target = target;
        this.registry = registry;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();
        GrantRepository grantRepository = this.registry.get(GrantRepository.class);
        Set<Grant> grants = grantRepository.findByPlayer(this.target.getUniqueId());

        // Çerçeveyi oluştur
        UIComponent frame = new ButtonBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) components.put(i, frame);
        for (int i = 45; i < 54; i++) components.put(i, frame);

        if (grants.isEmpty()) {
            components.put(22, new ButtonBuilder(Material.BARRIER).name("<red>No Grants Found").build());
            return components;
        }

        List<Grant> sortedGrants = new ArrayList<>(grants);
        sortedGrants.sort(Comparator.comparing(Grant::getCreationTime).reversed());

        int slot = 10;
        for (Grant grant : sortedGrants) {
            if (slot > 43) break; // Sayfa başına en fazla 28 öğe
            components.put(slot++, createGrantButton(grant, viewer));
        }

        return components;
    }

    private UIComponent createGrantButton(Grant grant, Player viewer) {
        GrantRepository grantRepository = this.registry.get(GrantRepository.class);
        MenuManager menuManager = this.registry.get(MenuManager.class);

        // Önce gerekli tüm bilgileri hazırlayalım
        String issuerName = (grant.getIssuerUuid() == null) ? "Console" : Bukkit.getOfflinePlayer(grant.getIssuerUuid()).getName();
        String durationString = grant.isPermanent() ? "Permanent" : TimeUtil.formatDuration(grant.getDuration());
        String statusString = grant.isActive() ? "<green>Active" : "<red>Expired";

        // Lore listesini dinamik olarak oluşturalım
        List<String> loreLines = new ArrayList<>();
        loreLines.add("");
        loreLines.add("<gray>Status: " + statusString);
        loreLines.add("");
        loreLines.add("<dark_aqua>▪ <aqua>Granted To: <white>" + this.target.getName());
        loreLines.add("<dark_aqua>▪ <aqua>Granted By: <white>" + issuerName);
        loreLines.add("");
        loreLines.add("<dark_aqua>▪ <aqua>Duration: <white>" + durationString);
        loreLines.add("<dark_aqua>▪ <aqua>Granted At: <white>" + DATE_FORMATTER.format(grant.getCreationTime()));
        if (!grant.isPermanent()) {
            loreLines.add("<dark_aqua>▪ <aqua>Expires At: <white>" + DATE_FORMATTER.format(grant.getExpiry()));
        }
        loreLines.add("");
        loreLines.add("<red>Right-click to revoke this grant.");
        loreLines.add("<dark_gray>ID: " + grant.getUniqueId().toString().substring(0, 8));

        // Şimdi ButtonBuilder ile her şeyi bir araya getirelim
        ButtonBuilder builder = new ButtonBuilder(grant.isActive() ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name("<gradient:#5e4fa2:#f79459>" + grant.getRankName())
                .lore(loreLines.toArray(new String[0])) // Listeyi diziye çevirip veriyoruz
                .onClick(event -> {
                    if (!event.getClick().isRightClick()) return;

                    grantRepository.delete(grant);
                    viewer.sendMessage(Component.text("Grant revoked.", NamedTextColor.GREEN));
                    this.refresh(); // Menüyü anında yenile!
                });

        if(grant.isActive()) {
            builder.enchant();
        }

        return builder.build();
    }
}