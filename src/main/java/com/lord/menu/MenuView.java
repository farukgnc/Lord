package com.lord.menu;

import com.lord.menu.components.UIComponent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

@Getter
public abstract class MenuView implements UIComponent {

    private final String title;
    private final int rows;

    @Getter(AccessLevel.PROTECTED)
    private Map<Integer, UIComponent> composedComponents;

    @Setter(AccessLevel.PACKAGE)
    private Player viewer;

    @Setter(AccessLevel.PACKAGE)
    private MenuManager menuManager;

    public MenuView(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    public abstract Map<Integer, UIComponent> compose(Player player);

    public void build(Player player) {
        this.composedComponents = compose(player);
    }

    public final void refresh() {
        if (this.viewer != null && this.menuManager != null) {
            this.menuManager.update(this.viewer);
        }
    }

    @Override
    public ItemStack render(Player player) {
        return null;
    }

    @Override
    public Consumer<InventoryClickEvent> getAction() {
        return event -> {};
    }
}
