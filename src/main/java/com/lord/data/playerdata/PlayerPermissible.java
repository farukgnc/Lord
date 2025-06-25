package com.lord.data.playerdata;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

public final class PlayerPermissible extends PermissibleBase {

    private final Player player;
    private final PlayerDataCache playerDataCache;
    private final PermissibleBase oldPermissible;

    public PlayerPermissible(Player player, PlayerDataCache playerDataCache, PermissibleBase oldPermissible) {
        super(player);
        this.player = player;
        this.playerDataCache = playerDataCache;
        this.oldPermissible = oldPermissible;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        // Önce bizim akıllı önbellek sistemimiz cevabı versin.
        boolean result = this.playerDataCache.hasPermission(this.player, permission);

        // Bizim sistemimiz 'true' derse, sonuç nettir.
        // Bizim sistemimiz 'false' derse (yani özel bir kural yoksa),
        // Bukkit'in varsayılan davranışına (op kontrolü gibi) saygı duymak için
        // eski beyne de bir soralım.
        return result || this.oldPermissible.hasPermission(permission);
    }
}
