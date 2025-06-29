package com.lord.grant.menus;

import com.lord.menu.MenuManager;
import com.lord.rank.Rank;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

@Getter
@Setter
public final class GrantWizard {

    private final ServiceRegistry registry;
    private final Player issuer; // Sihirbazı başlatan oyuncu
    private final UUID targetUuid; // Değişiklik: OfflinePlayer -> UUID
    private final String targetName; // Değişiklik: Hedefin ismini ayrıca tutuyoruz.

    // Adım adım doldurulan veriler
    private Rank selectedRank;
    private Duration selectedDuration;

    public GrantWizard(ServiceRegistry registry, Player issuer, UUID targetUuid, String targetName) {
        this.registry = registry;
        this.issuer = issuer;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    /**
     * Sihirbazı, rütbe seçme menüsüyle başlatır.
     */
    public void start() {
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new RankSelectionMenu(this));
    }

    /**
     * Sihirbazı, süre seçme menüsüne ilerletir.
     */
    public void advanceToDurationSelection() {
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new DurationSelectionMenu(this));
    }

    /**
     * Sihirbazı, son onay menüsüne ilerletir.
     */
    public void advanceToConfirmation() {
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new GrantConfirmationMenu(this));
    }
}