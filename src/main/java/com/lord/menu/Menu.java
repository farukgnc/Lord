package com.lord.menu;

import com.lord.menu.buttons.MenuButton;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;

@Getter
public abstract class Menu {

    private final String title;
    private final int rows;

    public Menu(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    /**
     * Bu metot, menü her açıldığında çağrılır.
     * Hangi slotta hangi butonun olacağını tanımlar.
     * Oyuncuya özel menüler oluşturmak için Player nesnesini parametre olarak alır.
     *
     * @param player Menüyü açan oyuncu.
     * @return Slot-Buton eşleşmesini içeren bir Map.
     */
    public abstract Map<Integer, MenuButton> getButtons(Player player);

}