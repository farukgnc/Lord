package com.lord.menu.utils;

import com.lord.menu.components.ButtonComponent;
import com.lord.menu.components.UIComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ButtonBuilder {

    private final ItemStack item;
    private Consumer<InventoryClickEvent> action = e -> {}; // Varsayılan eylem: hiçbir şey yapma

    public ButtonBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ButtonBuilder name(String miniMessage) {
        ItemMeta meta = this.item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(miniMessage));
        this.item.setItemMeta(meta);
        return this;
    }

    public ButtonBuilder lore(String... lines) {
        ItemMeta meta = this.item.getItemMeta();
        List<Component> lore = Arrays.stream(lines)
                .map(line -> MiniMessage.miniMessage().deserialize("<white>" + line))
                .collect(Collectors.toList());
        meta.lore(lore);
        this.item.setItemMeta(meta);
        return this;
    }

    public ButtonBuilder enchant() {
        this.item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
        ItemMeta meta = this.item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.item.setItemMeta(meta);
        return this;
    }

    public ButtonBuilder onClick(Consumer<InventoryClickEvent> action) {
        this.action = action;
        return this;
    }

    public UIComponent build() {
        return new ButtonComponent(this.item, this.action);
    }
}