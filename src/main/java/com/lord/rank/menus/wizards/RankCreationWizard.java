package com.lord.rank.menus.wizards;

import com.lord.menu.MenuManager;
import com.lord.rank.Rank;
import com.lord.rank.menus.ParentSelectionMenu;
import com.lord.rank.menus.RankConfirmationMenu;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ChatInputManager;
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
    private final ChatInputManager chatInputManager;

    private String name;
    private int priority;
    private String prefix;
    private String suffix;
    private final Set<String> selectedParentNames = new HashSet<>();

    public RankCreationWizard(ServiceRegistry registry, Player issuer) {
        this.registry = registry;
        this.issuer = issuer;
        this.chatInputManager = registry.get(ChatInputManager.class);
    }

    public void start() {
        promptForName();
    }

    private void promptForName() {
        issuer.closeInventory();
        issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<green>Please type the name for the new rank in chat.\n<gray>Type <white>'cancel'</white> to abort.\n"
        ));

        this.chatInputManager.prompt(issuer, input -> {
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

    private void promptForPriority() {
        Component message = MiniMessage.miniMessage().deserialize(
                "\n<green>Please type the priority for the rank '<white><rank></white>'.\n<gray>(Higher number = higher priority)\n",
                Placeholder.unparsed("rank", this.name)
        );
        issuer.sendMessage(message);

        this.chatInputManager.prompt(issuer, input -> {
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

    private void promptForPrefix() {
        promptForText("prefix",
                input -> this.prefix = input, // Sonucu ne yapacağı
                this::promptForSuffix         // Bir sonraki adım
        );
    }

    private void promptForSuffix() {
        promptForText("suffix",
                input -> this.suffix = input, // Sonucu ne yapacağı
                this::promptForParents        // Bir sonraki adım
        );
    }

    private void promptForText(String property, Consumer<String> onResult, Runnable nextStep) {
        // Renk listesini dinamik olarak oluştur
        String colorList = NamedTextColor.NAMES.keys().stream()
                .sorted()
                .map(name -> "<" + name + ">" + name)
                .collect(Collectors.joining("<gray>, </gray>"));
        Component hoverText = MiniMessage.miniMessage().deserialize("<gold>Available Colors:</gold>\n" + colorList);

        // Mesajı oluştur
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

        // Kullanıcıdan girdi iste
        this.chatInputManager.prompt(issuer, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                issuer.sendMessage(Component.text("Creation cancelled.", NamedTextColor.YELLOW));
                return;
            }
            if (!input.equalsIgnoreCase("none")) {
                onResult.accept(input); // Gelen eylemi çalıştır (prefix veya suffix'i ayarla)

                issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_green>»</dark_green> <gray>"+ capitalize(property) +" set to: <reset><value>",
                        Placeholder.component("value", MiniMessage.miniMessage().deserialize(input))
                ));
            } else {
                onResult.accept(null); // 'none' girildiyse null olarak ayarla
                issuer.sendMessage(MiniMessage.miniMessage().deserialize("<dark_green>»</dark_green> <gray>No " + property + " was set."));
            }

            // Her durumda, bir sonraki adıma geç
            nextStep.run();
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void promptForParents() {
        issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<green>Now, please select the parent rank(s) from the menu.\n"
        ));
        registry.get(MenuManager.class).open(issuer, new ParentSelectionMenu(this));
    }

    public void advanceToConfirmation() {
        registry.get(MenuManager.class).open(issuer, new RankConfirmationMenu(this));
    }

    public void createRank() {
        RankRepository rankRepository = registry.get(RankRepository.class);

        if (rankRepository.findByName(this.name).isPresent()) {
            issuer.sendMessage(Component.text("A rank with this name already exists. Creation cancelled.", NamedTextColor.RED));
            return;
        }

        Rank newRank = new Rank(this.name);
        newRank.setPriority(this.priority);
        if (this.prefix != null) newRank.setPrefix(this.prefix);
        if (this.suffix != null) newRank.setSuffix(this.suffix);
        newRank.getParentRankNames().addAll(this.selectedParentNames);

        rankRepository.save(newRank);
        issuer.sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_green>»</dark_green> <green>Rank <white><rank_name></white> has been created successfully!",
                Placeholder.unparsed("rank_name", this.name)
        ));
    }
}
