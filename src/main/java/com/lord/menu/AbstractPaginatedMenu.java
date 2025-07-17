package com.lord.menu;

import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.service.ServiceRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPaginatedMenu<T> extends MenuView {

    protected final ServiceRegistry registry;
    protected int page = 0;
    protected int itemsPerPage = 28; // 4 satır x 7 sütun

    public AbstractPaginatedMenu(ServiceRegistry registry, String title, int rows) {
        super(title, rows);
        this.registry = registry;
    }

    // --- Soyut Metotlar (Alt sınıflar bunları doldurmak zorunda) ---

    /**
     * Menüde listelenecek tüm öğelerin tam listesini döndürür.
     */
    public abstract List<T> getElements();

    /**
     * Listedeki tek bir öğeyi (T) alıp, onu menüde gösterilecek bir UIComponent'e çevirir.
     */
    public abstract UIComponent convertElement(T element);

    // --- Somut Metotlar (Tüm sayfalı menüler için ortak mantık) ---

    @Override
    public Map<Integer, UIComponent> compose(Player viewer) {
        Map<Integer, UIComponent> components = new HashMap<>();
        List<T> elements = getElements();

        // Çerçeve
        UIComponent frame = new ButtonBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) components.put(i, frame);
        for (int i = (getRows() * 9) - 9; i < getRows() * 9; i++) components.put(i, frame);

        // Sayfa Butonları
        if (page > 0) {
            UIComponent prevButton = new ButtonBuilder(Material.ARROW)
                    .name("<red>Previous Page")
                    .lore("<gray>Go to page " + page)
                    .onClick(event -> {
                        this.page--;
                        this.refresh();
                    }).build();
            components.put((getRows() * 9) - 9, prevButton);
        }

        int maxPage = (int) Math.ceil((double) elements.size() / itemsPerPage) - 1;
        if (page < maxPage) {
            UIComponent nextButton = new ButtonBuilder(Material.ARROW)
                    .name("<green>Next Page")
                    .lore("<gray>Go to page " + (page + 2))
                    .onClick(event -> {
                        this.page++;
                        this.refresh();
                    }).build();
            components.put((getRows() * 9) - 1, nextButton);
        }

        // Mevcut sayfadaki öğeleri yerleştir
        int startIndex = page * itemsPerPage;
        int slot = 10;
        for (int i = startIndex; i < startIndex + itemsPerPage && i < elements.size(); i++) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2; // Kenar boşlukları
            components.put(slot++, convertElement(elements.get(i)));
        }

        return components;
    }
}
