package com.lord.punishment.menus;

import com.lord.Lord;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentStatusFilter;
import com.lord.punishment.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;
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
import java.util.stream.Collectors;

public final class PunishmentsMenu extends MenuView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final UUID targetUuid;
    private final String targetName;
    private final ServiceRegistry registry;
    private final Lord plugin;
    private final PunishmentRepository punishmentRepository;

    // Filtreleme ve sayfalama durumu
    private List<Punishment> elements;
    private int currentPage = 1;
    private final int itemsPerPage = 28;
    private PunishmentStatusFilter statusFilter = PunishmentStatusFilter.ALL;
    private PunishmentType typeFilter = null; // null = hepsi

    public PunishmentsMenu(UUID targetUuid, String targetName, List<Punishment> initialPunishments, ServiceRegistry registry) {
        super("History: " + targetName, 6);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.elements = initialPunishments;
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.punishmentRepository = registry.get(PunishmentRepository.class);
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();
        int maxPage = (int) Math.ceil((double) elements.size() / itemsPerPage);
        if (maxPage == 0) maxPage = 1;

        // --- Çerçeve ve Kontrol Butonları ---
        UIComponent frame = new ButtonBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) components.put(i, frame);
        for (int i = 45; i < 54; i++) components.put(i, frame);

        components.put(4, new ButtonBuilder(Material.PLAYER_HEAD).name("<gold>" + this.targetName).build());
        components.put(49, new ButtonBuilder(Material.PAPER).name("<yellow>Page " + currentPage + "/" + maxPage).build());

        if (currentPage > 1) {
            components.put(48, new ButtonBuilder(Material.ARROW).name("<green>Previous Page").onClick(e -> {
                currentPage--;
                this.refresh();
            }).build());
        }
        if (currentPage < maxPage) {
            components.put(50, new ButtonBuilder(Material.ARROW).name("<green>Next Page").onClick(e -> {
                currentPage++;
                this.refresh();
            }).build());
        }

        // --- Filtre Butonları ---
        components.put(2, createStatusFilterButton());
        components.put(6, createTypeFilterButton());

        // --- İçerik ---
        if (elements.isEmpty()) {
            components.put(22, new ButtonBuilder(Material.BARRIER).name("<red>No Punishments Found").lore("<gray>Try changing the filters.").build());
        } else {
            int startIndex = (currentPage - 1) * itemsPerPage;
            List<Punishment> pageElements = elements.stream().skip(startIndex).limit(itemsPerPage).collect(Collectors.toList());

            int slot = 10;
            for (Punishment punishment : pageElements) {
                if (slot == 17 || slot == 26 || slot == 35) slot += 2; // Kenar boşlukları
                if (slot > 43) break;
                components.put(slot++, convertElement(punishment));
            }
        }
        return components;
    }

    private UIComponent createStatusFilterButton() {
        return new ButtonBuilder(Material.COMPARATOR)
                .name("<aqua>Filter by Status")
                .lore(
                        "<gray>Current: <white>" + statusFilter.getDisplayName(),
                        "",
                        "<yellow>Click to cycle."
                )
                .onClick(e -> {
                    this.statusFilter = this.statusFilter.next();
                    reloadDataAndRefresh();
                }).build();
    }

    private UIComponent createTypeFilterButton() {
        List<PunishmentType> types = new ArrayList<>(Arrays.asList(PunishmentType.values()));
        int currentIndex = (typeFilter == null) ? -1 : typeFilter.ordinal();

        return new ButtonBuilder(Material.HOPPER)
                .name("<aqua>Filter by Type")
                .lore(
                        "<gray>Current: <white>" + (typeFilter == null ? "All" : typeFilter.name()),
                        "",
                        "<yellow>Click to cycle."
                )
                .onClick(e -> {
                    int nextIndex = currentIndex + 1;
                    if (nextIndex >= types.size()) {
                        this.typeFilter = null; // Listenin sonundan sonra "All" durumuna dön
                    } else {
                        this.typeFilter = types.get(nextIndex);
                    }
                    reloadDataAndRefresh();
                }).build();
    }

    private void reloadDataAndRefresh() {
        if (this.getViewer() != null && this.getViewer().isOnline()) {
            this.getViewer().sendActionBar(Component.text("Loading history...", NamedTextColor.YELLOW));
        }

        this.punishmentRepository.findWithFilters(this.targetUuid, this.statusFilter, this.typeFilter)
                .thenAccept(newElements -> {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        this.elements = newElements;
                        this.currentPage = 1;
                        this.refresh();
                    });
                });
    }

    private UIComponent convertElement(Punishment punishment) {
        String issuerName = "Console";
        if (punishment.getIssuerUuid() != null) {
            // BU KISIM PLAYERRESOLVER OLMALI
            issuerName = Optional.ofNullable(Bukkit.getOfflinePlayer(punishment.getIssuerUuid()).getName()).orElse("Unknown");
        }
        String durationString = punishment.isPermanent() ? "Permanent" : TimeUtil.formatDuration(punishment.getDuration());

        Material material;
        String statusString;
        String typeColor;

        if (punishment.isActive()) {
            statusString = "<green>Active";
            material = Material.ENCHANTED_BOOK;
            typeColor = "<red>";
        } else {
            statusString = "<gray>Expired/Inactive";
            material = Material.WRITTEN_BOOK;
            typeColor = "<gray>";
        }

        if (punishment.getType() == PunishmentType.KICK || punishment.getType() == PunishmentType.WARN) {
            material = Material.PAPER;
            typeColor = "<yellow>";
        }

        List<String> loreLines = new ArrayList<>();
        loreLines.add("");
        loreLines.add("<gray>Status: " + statusString);
        loreLines.add("<dark_aqua>▪ <aqua>Punished By: <white>" + issuerName);
        loreLines.add("<dark_aqua>▪ <aqua>Reason: <white>" + punishment.getReason());
        loreLines.add("");
        loreLines.add("<dark_aqua>▪ <aqua>Duration: <white>" + durationString);
        loreLines.add("<dark_aqua>▪ <aqua>Given At: <white>" + DATE_FORMATTER.format(punishment.getCreationTime()));
        if (!punishment.isPermanent()) {
            loreLines.add("<dark_aqua>▪ <aqua>Expires At: <white>" + DATE_FORMATTER.format(punishment.getExpiry()));
        }
        loreLines.add("");
        loreLines.add("<dark_gray>ID: " + punishment.getUniqueId().toString().substring(0, 8));

        ButtonBuilder builder = new ButtonBuilder(material)
                .name(typeColor + "<bold>" + punishment.getType().name())
                .lore(loreLines.toArray(new String[0]));

        if (punishment.isActive() && material == Material.ENCHANTED_BOOK) {
            builder.enchant();
        }

        return builder.build();
    }
}