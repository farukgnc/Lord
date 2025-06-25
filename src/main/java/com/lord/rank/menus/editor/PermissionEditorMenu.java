package com.lord.rank.menus.editor;

import com.lord.menu.AbstractPaginatedMenu;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ChatInputManager;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PermissionEditorMenu extends AbstractPaginatedMenu<String> {

    private final Rank rank;

    public PermissionEditorMenu(Rank rank, ServiceRegistry registry) {
        super(registry, "Permissions for: " + rank.getName(), 6);
        this.rank = rank;
    }

    @Override
    public List<String> getElements() {
        // Rütbenin izinlerini alfabetik olarak sıralayarak listele.
        List<String> permissions = new ArrayList<>(this.rank.getPermissions());
        permissions.sort(String.CASE_INSENSITIVE_ORDER);
        return permissions;
    }

    @Override
    public UIComponent convertElement(String permission) {
        // Her bir izin metnini, tıklanabilir bir butona çevir.
        boolean isNegated = permission.startsWith("-");
        String cleanPermission = isNegated ? permission.substring(1) : permission;

        return new ButtonBuilder(isNegated ? Material.REDSTONE : Material.EMERALD)
                .name((isNegated ? "<red>" : "<green>") + cleanPermission)
                .lore(
                        "",
                        "<yellow>Right-click to remove this permission."
                )
                .onClick(event -> {
                    if (event.getClick().isRightClick()) {
                        this.rank.getPermissions().remove(permission);
                        this.registry.get(RankRepository.class).save(this.rank);
                        this.refresh(); // Menüyü anında yenile.
                    }
                })
                .build();
    }

    // Alt bardaki özel butonları eklemek için compose metodunu override ediyoruz.
    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        // Önce üst sınıfın (AbstractPaginatedMenu) sayfalama ve öğe yerleştirme mantığını çalıştır.
        Map<Integer, UIComponent> components = super.compose(viewer);

        // "İzin Ekle" butonu
        UIComponent addPermissionButton = new ButtonBuilder(Material.WRITABLE_BOOK)
                .name("<green>Add Permission")
                .lore("<gray>Click to add a new permission node.")
                .onClick(event -> {
                    ChatInputManager inputManager = registry.get(ChatInputManager.class);
                    viewer.closeInventory();
                    viewer.sendMessage(Component.text("Please type the permission node in chat.", NamedTextColor.GREEN));
                    viewer.sendMessage(Component.text("Prefix with '-' to negate (e.g., -essentials.fly).", NamedTextColor.GRAY));

                    inputManager.prompt(viewer, input -> {
                        this.rank.getPermissions().add(input.toLowerCase());
                        this.registry.get(RankRepository.class).save(this.rank);
                        viewer.sendMessage(Component.text("Permission '" + input + "' added.", NamedTextColor.GREEN));
                        this.getMenuManager().open(viewer, new PermissionEditorMenu(this.rank, this.registry));
                    });
                })
                .build();

        // "Geri Dön" butonu
        UIComponent backButton = new ButtonBuilder(Material.BARRIER)
                .name("<red>Back to Editor")
                .onClick(event -> {
                    this.getMenuManager().open(viewer, new RankEditorMenu(this.rank, this.registry));
                })
                .build();

        components.put(48, addPermissionButton); // Sol tarafa yakın
        components.put(49, backButton);      // Ortada

        return components;
    }
}
