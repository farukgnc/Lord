package com.lord.data.playerdata;

import com.lord.data.CachedData;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
        // 1. Önbellekten oyuncunun hesaplanmış verisini al. Bu anlık bir işlemdir.
        Optional<CachedData> cachedDataOpt = this.playerDataCache.getPlayerData(this.player.getUniqueId());

        if (cachedDataOpt.isPresent()) {
            // 2. Eğer veri varsa, asıl izin kontrolünü bu veri üzerinden yap.
            // Bu, bizim grant ve rank sistemimizden gelen sonucu verir.
            boolean hasPerm = cachedDataOpt.get().getPermissionData().hasPermission(permission);

            // Bizim sistemimiz 'true' derse, sonuç nettir, oyuncunun izni vardır.
            if (hasPerm) {
                return true;
            }
        }

        // 3. Bizim sistemimiz 'false' dediyse VEYA oyuncunun verisi bir şekilde önbellekte yoksa,
        // Bukkit'in varsayılan davranışlarına (op kontrolü, '*' izni gibi) saygı duymak için
        // orijinal (eski) izin denetleyicisine de bir soralım.
        return this.oldPermissible.hasPermission(permission);
    }
}