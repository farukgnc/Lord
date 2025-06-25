package com.lord.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String miniMessage) {
        this.meta.displayName(MiniMessage.miniMessage().deserialize(miniMessage));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<Component> lore = Arrays.stream(lines)
                // Her satır için varsayılan rengi <gray> yapalım, ama MiniMessage tag'leri bunu ezebilir.
                .map(line -> MiniMessage.miniMessage().deserialize("<gray>" + line))
                .collect(Collectors.toList());
        this.meta.lore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = lines.stream()
                .map(line -> MiniMessage.miniMessage().deserialize("<gray>" + line))
                .collect(Collectors.toList());
        this.meta.lore(lore);
        return this;
    }

    public ItemBuilder enchant() {
        this.meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
        this.meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder hideAttributes() {
        this.meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }
}
