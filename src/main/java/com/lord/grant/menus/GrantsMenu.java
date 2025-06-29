package com.lord.grant.menus;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.Grant;
import com.lord.grant.repositories.GrantRepository;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.grant.GrantCacheService;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class GrantsMenu extends MenuView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final String targetName;
    private final Set<Grant> grants;
    private final ServiceRegistry registry;
    private final Lord plugin;

    public GrantsMenu(UUID targetUuid, String targetName, Set<Grant> grants, ServiceRegistry registry) {
        super("Grants: " + targetName, 6);
        this.targetName = targetName;
        this.grants = grants;
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();

        UIComponent frame = new ButtonBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) components.put(i, frame);
        for (int i = 45; i < 54; i++) components.put(i, frame);

        if (grants.isEmpty()) {
            components.put(22, new ButtonBuilder(Material.BARRIER).name("<red>No Grants Found").build());
            return components;
        }

        List<Grant> sortedGrants = new ArrayList<>(this.grants);
        sortedGrants.sort(Comparator.comparing(Grant::getCreationTime).reversed());

        int slot = 10;
        for (Grant grant : sortedGrants) {
            if (slot > 43) break;
            components.put(slot++, createGrantButton(grant, viewer));
        }

        return components;
    }

    private UIComponent createGrantButton(Grant grant, Player viewer) {
        GrantRepository grantRepository = this.registry.get(GrantRepository.class);

        String issuerName = "Console";
        if (grant.getIssuerUuid() != null) {
            // Sadece ismi almak için OfflinePlayer kullanmak güvenlidir.
            issuerName = Bukkit.getOfflinePlayer(grant.getIssuerUuid()).getName();
        }

        String durationString = grant.isPermanent() ? "Permanent" : TimeUtil.formatDuration(grant.getDuration());
        String statusString = grant.isActive() ? "<green>Active" : "<red>Expired";

        List<String> loreLines = new ArrayList<>();
        loreLines.add("");
        loreLines.add("<gray>Status: " + statusString);
        loreLines.add("");
        loreLines.add("<dark_aqua>▪ <aqua>Granted To: <white>" + this.targetName);
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

        ButtonBuilder builder = new ButtonBuilder(grant.isActive() ? Material.ENCHANTED_BOOK : Material.BOOK)
                .name("<gradient:#5e4fa2:#f79459>" + grant.getRankName())
                .lore(loreLines.toArray(new String[0]))
                .onClick(event -> {
                    if (!event.getClick().isRightClick()) return;

                    GrantCacheService grantCacheService = this.registry.get(GrantCacheService.class);

                    // 1. Grant'i asenkron olarak sil.
                    grantRepository.delete(grant).thenRun(() -> {
                        // 2. Silme işlemi bittiğinde, grant'in sahibinin önbelleğini geçersiz kıl.
                        grantCacheService.invalidate(grant.getGranteeUuid());

                        // 3. Ayrıca, izinlerin yeniden hesaplanması için PlayerDataCache'i de geçersiz kıl.
                        this.registry.get(PlayerDataCache.class).invalidate(grant.getGranteeUuid());

                        // menu güncellenmesi için
                        this.grants.remove(grant);

                        // 4. Ana thread'e dönerek kullanıcıya mesaj gönder ve menüyü yenile.
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            viewer.sendMessage(Component.text("Grant revoked.", NamedTextColor.GREEN));
                            this.refresh();
                        });
                    });
                });

        if(grant.isActive()) {
            builder.enchant();
        }

        return builder.build();
    }
}