package com.lord.rank.menus.creation;

import com.lord.chat.ChatService;
import com.lord.menu.MenuManager;
import com.lord.rank.RankModule;
import com.lord.rank.exceptions.RankAlreadyExistsException;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public final class RankCreationWizard {

    private final ServiceRegistry registry;
    private final Player issuer;
    private final ChatService chatService;

    // Data collected throughout the wizard
    private String name;
    private int priority;
    private String prefix;
    private String suffix;
    private final Set<String> selectedParentNames = new HashSet<>();

    public RankCreationWizard(ServiceRegistry registry, Player issuer) {
        this.registry = registry;
        this.issuer = issuer;
        this.chatService = registry.get(ChatService.class);
    }

    /**
     * Starts the wizard by prompting for the rank name.
     */
    public void start() {
        promptForName();
    }

    /**
     * Step 1: Prompts the user to enter the rank name in chat.
     */
    private void promptForName() {
        issuer.closeInventory();
        issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<green>Please type the name for the new rank in chat.\n<gray>Type <white>'cancel'</white> to abort.\n"
        ));

        this.chatService.prompt(issuer, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                issuer.sendMessage(Component.text("Creation cancelled.", NamedTextColor.YELLOW));
                return;
            }
            this.name = input.toLowerCase();
            issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_green>»</dark_green> <gray>Rank name set to: <white><name>",
                    Placeholder.unparsed("name", this.name)
            ));
            promptForPriority();
        });
    }

    /**
     * Step 2: Prompts the user to enter the rank priority.
     */
    private void promptForPriority() {
        Component message = MiniMessage.miniMessage().deserialize(
                "\n<green>Please type the priority for the rank '<white><rank></white>'.\n<gray>(Higher number = higher priority)\n",
                Placeholder.unparsed("rank", this.name)
        );
        issuer.sendMessage(message);

        this.chatService.prompt(issuer, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                issuer.sendMessage(Component.text("Creation cancelled.", NamedTextColor.YELLOW));
                return;
            }
            try {
                this.priority = Integer.parseInt(input);
                issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_green>»</dark_green> <gray>Priority set to: <yellow><priority>",
                        Placeholder.unparsed("priority", String.valueOf(this.priority))
                ));
                promptForPrefix();
            } catch (NumberFormatException e) {
                issuer.sendMessage(Component.text("Invalid number. Please try again.", NamedTextColor.RED));
                promptForPriority();
            }
        });
    }

    /**
     * Step 3: Prompts the user to enter the rank prefix.
     */
    private void promptForPrefix() {
        promptForText("prefix",
                input -> this.prefix = input,
                this::promptForSuffix
        );
    }

    /**
     * Step 4: Prompts the user to enter the rank suffix.
     */
    private void promptForSuffix() {
        promptForText("suffix",
                input -> this.suffix = input,
                this::promptForParents
        );
    }

    /**
     * Step 5: Opens the parent selection menu.
     */
    private void promptForParents() {
        issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<green>Now, please select the parent rank(s) from the menu.\n"
        ));
        registry.get(MenuManager.class).open(issuer, new ParentSelectionMenu(this));
    }

    /**
     * Step 6: Advances the wizard to the final confirmation menu.
     * This is called by the "Done" button in the ParentSelectionMenu.
     */
    public void advanceToConfirmation() {
        registry.get(MenuManager.class).open(issuer, new RankConfirmationMenu(this));
    }

    public void createRank() {
        RankModule rankModule = registry.get(RankModule.class);

        try {
            rankModule.createRank(
                    this.name,
                    this.priority,
                    this.prefix,
                    this.suffix,
                    this.selectedParentNames
            );

            issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_green>»</dark_green> <green>Rank <white><rank_name></white> has been created successfully!",
                    Placeholder.unparsed("rank_name", this.name)
            ));

        } catch (RankAlreadyExistsException e) {
            issuer.sendMessage(Component.text("A rank with this name already exists. Creation cancelled.", NamedTextColor.RED));
        }
    }

    /**
     * A helper method to reduce code duplication for prompting text input (prefix/suffix).
     */
    private void promptForText(String property, Consumer<String> onResult, Runnable nextStep) {
        String colorList = NamedTextColor.NAMES.keys().stream()
                .sorted()
                .map(name -> "<" + name + ">" + name)
                .collect(Collectors.joining("<gray>, </gray>"));
        Component hoverText = MiniMessage.miniMessage().deserialize("<gold>Available Colors:</gold>\n" + colorList);

        Component message = Component.text()
                .append(Component.newline())
                .append(MiniMessage.miniMessage().deserialize("<green>Please type the <property> for the rank '<white>" + this.name + "</white>'.", Placeholder.unparsed("property", property)))
                .append(Component.newline())
                .append(MiniMessage.miniMessage().deserialize("<gray>You can use "))
                .append(Component.text("color tags", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(hoverText)))
                .append(MiniMessage.miniMessage().deserialize(" <gray>for colors. Type <white>'none'</white> to skip.\n"))
                .build();

        issuer.sendMessage(message);

        this.chatService.prompt(issuer, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                issuer.sendMessage(Component.text("Creation cancelled.", NamedTextColor.YELLOW));
                return;
            }

            String result = input.equalsIgnoreCase("none") ? null : input;
            onResult.accept(result);

            if (result != null) {
                issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_green>»</dark_green> <gray>"+ capitalize(property) +" set to: <reset><value>",
                        Placeholder.component("value", MiniMessage.miniMessage().deserialize(result))
                ));
            } else {
                issuer.sendMessage(MiniMessage.miniMessage().deserialize("<dark_green>»</dark_green> <gray>No " + property + " was set."));
            }

            nextStep.run();
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
