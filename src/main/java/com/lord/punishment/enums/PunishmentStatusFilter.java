package com.lord.punishment.enums;

public enum PunishmentStatusFilter {
    ALL("All"),
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String displayName;

    PunishmentStatusFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Toggle metodu: Bir sonraki duruma geçer (ALL -> ACTIVE -> INACTIVE -> ALL)
    public PunishmentStatusFilter next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}