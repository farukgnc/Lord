package com.lord.grant.menus;

import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.Grant;
import com.lord.grant.repositories.GrantRepository;
import com.lord.menu.MenuView;
import com.lord.menu.components.UIComponent;
import com.lord.menu.utils.ButtonBuilder;
import com.lord.grant.GrantCacheService;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GrantConfirmationMenu extends MenuView {

    private final GrantWizard wizard;

    public GrantConfirmationMenu(GrantWizard wizard) {
        super("Confirm Grant", 3);
        this.wizard = wizard;
    }

    @Override
    public Map<Integer, UIComponent> compose(Player player) {
        Map<Integer, UIComponent> components = new HashMap<>();

        // Onay Butonu
        components.put(11, new ButtonBuilder(Material.GREEN_WOOL)
                .name("<green>Confirm Grant")
                .lore(
                        "<gray>Click to confirm and grant the rank."
                )
                .onClick(event -> {
                    GrantRepository grantRepository = wizard.getRegistry().get(GrantRepository.class);
                    UUID issuerUuid = wizard.getIssuer().getUniqueId();

                    Grant newGrant = new Grant(
                            wizard.getTargetUuid(),
                            wizard.getSelectedRank().getName(),
                            issuerUuid,
                            wizard.getSelectedDuration()
                    );

                    grantRepository.save(newGrant).thenRun(() -> {
                        // 2. Kaydetme işlemi bittiğinde, hedef oyuncunun grant önbelleğini geçersiz kıl.
                        wizard.getRegistry().get(GrantCacheService.class).invalidate(wizard.getTargetUuid());

                        // 3. Ayrıca, izinlerin yeniden hesaplanması için PlayerDataCache'i de geçersiz kıl.
                        // Bu, oyuncu online ise anında yeni izinlerini almasını sağlar.
                        wizard.getRegistry().get(PlayerDataCache.class).invalidate(wizard.getTargetUuid());

                        player.sendMessage(Component.text("Grant successful!", NamedTextColor.GREEN));
                    });

                    player.closeInventory();
                })
                .build());

        // Bilgi Paneli
        String durationString = wizard.getSelectedDuration().isZero() ? "Permanent" : TimeUtil.formatDuration(wizard.getSelectedDuration());
        components.put(13, new ButtonBuilder(Material.PAPER)
                .name("<yellow>Grant Summary")
                .lore(
                        "<gray>Target: <white>" + wizard.getTargetName(),
                        "<gray>Rank: <white>" + wizard.getSelectedRank().getName(),
                        "<gray>Duration: <white>" + durationString
                )
                .build());

        // İptal Butonu
        components.put(15, new ButtonBuilder(Material.RED_WOOL)
                .name("<red>Cancel")
                .lore(
                        "<gray>Click to cancel the process."
                )
                .onClick(event -> player.closeInventory())
                .build());

        return components;
    }
}
