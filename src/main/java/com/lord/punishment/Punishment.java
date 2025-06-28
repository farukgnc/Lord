package com.lord.punishment;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public final class Punishment {

    private final UUID uniqueId;
    private final PunishmentType type;
    private final UUID punishedUuid;
    private final String reason;

    private final UUID issuerUuid; // Null olabilir (örn: konsol tarafından verildi)
    private final Instant creationTime;
    private final Duration duration; // Süresiz ise Duration.ZERO olabilir

    public Punishment(PunishmentType type, UUID punishedUuid, String reason, @Nullable UUID issuerUuid, Duration duration) {
        this.uniqueId = UUID.randomUUID();
        this.type = type;
        this.punishedUuid = punishedUuid;
        this.reason = reason;
        this.issuerUuid = issuerUuid;
        this.creationTime = Instant.now();
        this.duration = duration;
    }

    public boolean isPermanent() {
        // Kick ve Warn gibi cezaların süresi olmaz, onları da kalıcı kabul edebiliriz.
        if (this.type == PunishmentType.KICK || this.type == PunishmentType.WARN) {
            return true;
        }
        return this.duration.isZero();
    }

    public Instant getExpiry() {
        if (isPermanent()) {
            return Instant.MAX;
        }
        return this.creationTime.plus(this.duration);
    }

    public boolean isActive() {
        if (this.type == PunishmentType.KICK || this.type == PunishmentType.WARN) {
            return false; // Kick ve Warn anlıktır, hiçbir zaman "aktif" kalmazlar.
        }
        return getExpiry().isAfter(Instant.now());
    }
}
