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

    // Bu, her bir alt bileşenin nerede olduğunu saklayan haritadır.
    // Dışarıdan doğrudan erişilmesini istemediğimiz için 'protected'
    @Getter(AccessLevel.PROTECTED)
    private Map<Integer, UIComponent> components;

    // Durum yönetimi için: Bu menünün hangi oyuncu için açık olduğunu bilmemiz gerekir.
    @Setter(AccessLevel.PACKAGE)
    private Player viewer;

    // Durum yönetimi için: MenuManager'a erişim.
    @Setter(AccessLevel.PACKAGE)
    private MenuManager menuManager;

    public MenuView(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    /**
     * Bu metot, menünün içeriğini oluşturur.
     * Geliştirici, bu metodu doldurarak hangi slotta hangi component'in olacağını tanımlar.
     * @param player Menüyü görüntüleyen oyuncu.
     * @return Slot-Bileşen haritası.
     */
    public abstract Map<Integer, UIComponent> compose(Player player);

    /**
     * Durum değiştiğinde menüyü yenilemek için kullanılır.
     */
    public final void refresh() {
        if (this.viewer != null && this.menuManager != null) {
            this.menuManager.update(this.viewer);
        }
    }

    // UIComponent arayüzünden gelen metotlar (bir menünün kendisi tıklanabilir olmadığı için boş kalabilir)
    @Override
    public ItemStack render(Player player) {
        return null; // Bir menü, başka bir menünün içinde bir item olarak gösterilmez.
    }

    @Override
    public Consumer<InventoryClickEvent> getAction() {
        return event -> {}; // Tıklama eylemi yok.
    }
}