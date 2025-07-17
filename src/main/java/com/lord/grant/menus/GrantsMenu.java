package com.lord.grant.menus;

import com.lord.Lord;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.Grant;
import com.lord.grant.GrantService;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.service.ServiceRegistry;
import com.lord.utils.PlayerResolver;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class GrantsMenu extends MenuView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final String targetName;
    private final Set<Grant> grants;
    private final ServiceRegistry registry;
    private final Lord plugin;
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();
    private boolean initialComposition = true;

    public GrantsMenu(UUID targetUuid, String targetName, Set<Grant> grants, ServiceRegistry registry) {
        super("Grants: " + targetName, 6);
        this.targetName = targetName;
        this.grants = grants;
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        if (initialComposition) {
            initialComposition = false;
            loadNamesAndRecompose();
            return getLoadingView();
        }

        Map<Integer, UIComponent> components = new HashMap<>();
        addFrame(components);

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

    private void loadNamesAndRecompose() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Grant grant : grants) {
            if (grant.getIssuerUuid() != null && !nameCache.containsKey(grant.getIssuerUuid())) {
                futures.add(
                        PlayerResolver.resolveName(grant.getIssuerUuid()).thenAccept(nameOpt ->
                                nameOpt.ifPresent(name -> nameCache.put(grant.getIssuerUuid(), name)))
                );
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, this::refresh));
    }

    private Map<Integer, UIComponent> getLoadingView() {
        Map<Integer, UIComponent> components = new HashMap<>();
        addFrame(components);
        components.put(22, new ButtonBuilder(Material.CLOCK).name("<yellow>Loading grant details...").build());
        return components;
    }

    private void addFrame(Map<Integer, UIComponent> components) {
        UIComponent frame = new ButtonBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) components.put(i, frame);
        for (int i = 45; i < 54; i++) components.put(i, frame);
    }

    private UIComponent createGrantButton(Grant grant, Player viewer) {
        GrantService grantService = this.registry.get(GrantService.class);

        String issuerName = "Console";
        if (grant.getIssuerUuid() != null) {
            issuerName = nameCache.getOrDefault(grant.getIssuerUuid(), "Loading...");
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

                    // Use GrantService to remove the grant properly
                    grantService.removeGrant(grant.getUniqueId(), grant.getGranteeUuid(), this.targetName, viewer)
                        .thenAccept(success -> {
                            if (success) {
                                this.grants.remove(grant);
                                Bukkit.getScheduler().runTask(this.plugin, this::refresh);
                            }
                        });
                });

        if(grant.isActive()) {
            builder.enchant();
        }

        return builder.build();
    }
}
