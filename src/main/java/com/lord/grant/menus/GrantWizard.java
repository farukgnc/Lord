package com.lord.grant.menus;

import com.lord.menu.MenuManager;
import com.lord.rank.Rank;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Duration;

@Getter
@Setter
public final class GrantWizard {

    private final ServiceRegistry registry;
    private final Player issuer; // Sihirbazı başlatan oyuncu
    private final OfflinePlayer target; // Grant'in verileceği hedef

    // Adım adım doldurulan veriler
    private Rank selectedRank;
    private Duration selectedDuration;

    public GrantWizard(ServiceRegistry registry, Player issuer, OfflinePlayer target) {
        this.registry = registry;
        this.issuer = issuer;
        this.target = target;
    }

    public void start() {
        // Sihirbaz, Rank seçme menüsüyle başlar.
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new RankSelectionMenu(this));
    }

    public void advanceToDurationSelection() {
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new DurationSelectionMenu(this));
    }

    public void advanceToConfirmation() {
        MenuManager menuManager = registry.get(MenuManager.class);
        menuManager.open(issuer, new GrantConfirmationMenu(this));
    }
}
