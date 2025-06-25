package com.lord.rank.menus.editor;

import com.lord.menu.ConfirmationMenu;
import com.lord.menu.MenuView;
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

import java.util.HashMap;
import java.util.Map;

public final class RankEditorMenu extends MenuView {

    private final Rank rank;
    private final ServiceRegistry registry;
    private final RankRepository rankRepository;

    public RankEditorMenu(Rank rank, ServiceRegistry registry) {
        super("Editing Rank: " + rank.getName(), 5);
        this.rank = rank;
        this.registry = registry;
        this.rankRepository = registry.get(RankRepository.class);
    }

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Bilgi Paneli
        components.put(4, new ButtonBuilder(Material.NAME_TAG)
                .name("<gold>" + capitalize(rank.getName()))
                .lore(
                        "<gray>Priority: <yellow>" + rank.getPriority(),
                        "<gray>Prefix: " + (rank.getPrefix() != null ? rank.getPrefix() : "<i>None"),
                        "<gray>Suffix: " + (rank.getSuffix() != null ? rank.getSuffix() : "<i>None")
                ).build());

        // Düzenleme Butonları
        components.put(20, createPriorityButton());
        components.put(22, createPrefixButton());
        components.put(24, createSuffixButton());

        components.put(30, createParentsButton());
        components.put(32, createPermissionsButton());

        // Silme Butonu (eğer 'default' rütbesi değilse)
        if (!rank.getName().equalsIgnoreCase("default")) {
            components.put(40, createDeleteButton());
        }

        return components;
    }

    private UIComponent createPriorityButton() {
        return new ButtonBuilder(Material.DIAMOND)
                .name("<aqua>Edit Priority")
                .lore("<gray>Current: <yellow>" + this.rank.getPriority(), "", "<yellow>Click to change the priority.")
                .onClick(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    ChatInputManager inputManager = registry.get(ChatInputManager.class);

                    viewer.closeInventory();
                    viewer.sendMessage(Component.text("Please type the new priority in chat.", NamedTextColor.GREEN));

                    inputManager.prompt(viewer, input -> {
                        try {
                            int newPriority = Integer.parseInt(input);
                            this.rank.setPriority(newPriority);
                            this.rankRepository.save(this.rank);
                            viewer.sendMessage(Component.text("Priority updated successfully!", NamedTextColor.GREEN));
                            this.getMenuManager().open(viewer, new RankEditorMenu(this.rank, this.registry));
                        } catch (NumberFormatException e) {
                            viewer.sendMessage(Component.text("Invalid number. Please try again.", NamedTextColor.RED));
                            this.getMenuManager().open(viewer, new RankEditorMenu(this.rank, this.registry));
                        }
                    });
                }).build();
    }

    private UIComponent createPrefixButton() {
        return new ButtonBuilder(Material.OAK_SIGN)
                .name("<aqua>Edit Prefix")
                .lore("<gray>Current: " + (this.rank.getPrefix() != null ? this.rank.getPrefix() : "<i>None"), "", "<yellow>Click to change the prefix.")
                .onClick(event -> promptForText("prefix", (player, input) -> {
                    this.rank.setPrefix(input);
                    this.rankRepository.save(this.rank);
                    player.sendMessage(Component.text("Prefix updated successfully!", NamedTextColor.GREEN));
                    this.getMenuManager().open(player, new RankEditorMenu(this.rank, this.registry));
                })).build();
    }

    private UIComponent createSuffixButton() {
        return new ButtonBuilder(Material.OAK_SIGN)
                .name("<aqua>Edit Suffix")
                .lore("<gray>Current: " + (this.rank.getSuffix() != null ? this.rank.getSuffix() : "<i>None"), "", "<yellow>Click to change the suffix.")
                .onClick(event -> promptForText("suffix", (player, input) -> {
                    this.rank.setSuffix(input);
                    this.rankRepository.save(this.rank);
                    player.sendMessage(Component.text("Suffix updated successfully!", NamedTextColor.GREEN));
                    this.getMenuManager().open(player, new RankEditorMenu(this.rank, this.registry));
                })).build();
    }

    private UIComponent createParentsButton() {
        ButtonBuilder builder = new ButtonBuilder(Material.BOOKSHELF)
                .name("<aqua>Manage Parents")
                .onClick(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    this.getMenuManager().open(viewer, new RankParentEditMenu(this.rank, this.registry));
                });

        builder.lore("<gray>Current Parents:");
        if (rank.getParentRankNames().isEmpty()) {
            builder.lore("<gray><i>- None");
        } else {
            rank.getParentRankNames().forEach(parent -> builder.lore("<gray> - <white>" + parent));
        }
        builder.lore("", "<yellow>Click to manage parents.");

        return builder.build();
    }

    private UIComponent createPermissionsButton() {
        return new ButtonBuilder(Material.WRITABLE_BOOK)
                .name("<aqua>Manage Permissions")
                .lore(
                        "<gray>Current Permissions: <white>" + rank.getPermissions().size(),
                        "",
                        "<yellow>Click to edit permissions."
                )
                .onClick(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    this.getMenuManager().open(viewer, new PermissionEditorMenu(this.rank, this.registry));
                }).build();
    }

    private UIComponent createDeleteButton() {
        return new ButtonBuilder(Material.BARRIER)
                .name("<red><bold>Delete Rank")
                .lore("<dark_red>This action cannot be undone!")
                .onClick(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    ConfirmationMenu confirmationMenu = new ConfirmationMenu(
                            "Delete Rank: " + rank.getName(),
                            "Are you sure you want to permanently\ndelete this rank?",
                            onConfirm -> {
                                this.rankRepository.delete(this.rank.getName());
                                viewer.sendMessage(Component.text("Rank '" + this.rank.getName() + "' has been deleted.", NamedTextColor.GREEN));
                                this.getMenuManager().open(viewer, new RankListMenu(this.registry));
                            },
                            onCancel -> this.getMenuManager().open(viewer, new RankEditorMenu(this.rank, this.registry))
                    );
                    this.getMenuManager().open(viewer, confirmationMenu);
                }).build();
    }

    private void promptForText(String property, java.util.function.BiConsumer<Player, String> onResult) {
        Player viewer = this.getViewer();
        ChatInputManager inputManager = registry.get(ChatInputManager.class);

        viewer.closeInventory();
        viewer.sendMessage(Component.text("Please type the new " + property + " in chat. Use 'none' to remove.", NamedTextColor.GREEN));

        inputManager.prompt(viewer, input -> {
            onResult.accept(viewer, input.equalsIgnoreCase("none") ? null : input);
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
