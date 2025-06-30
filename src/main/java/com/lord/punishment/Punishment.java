package com.lord.punishment;

import lombok.Getter;
import lombok.Setter;
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
    private final UUID issuerUuid;
    private final Instant creationTime;
    private final Duration duration;

    // --- YENİ ALANLAR ---
    @Setter
    private boolean pardoned = false;
    @Setter
    private UUID pardonerUuid;
    @Setter
    private Instant pardonTime;

    /** Yeni ceza oluşturmak için */
    public Punishment(PunishmentType type, UUID punishedUuid, String reason, @Nullable UUID issuerUuid, Duration duration) {
        this.uniqueId = UUID.randomUUID();
        this.type = type;
        this.punishedUuid = punishedUuid;
        this.reason = reason;
        this.issuerUuid = issuerUuid;
        this.creationTime = Instant.now();
        this.duration = duration;
    }

    /** Veritabanından yeniden oluşturmak için */
    public Punishment(UUID uniqueId, PunishmentType type, UUID punishedUuid, String reason, @Nullable UUID issuerUuid, Instant creationTime, Duration duration) {
        this.uniqueId = uniqueId;
        this.type = type;
        this.punishedUuid = punishedUuid;
        this.reason = reason;
        this.issuerUuid = issuerUuid;
        this.creationTime = creationTime;
        this.duration = duration;
    }

    /**
     * Bir cezanın aktif olup olmadığını kontrol eder.
     * Artık affedilme durumunu da hesaba katar.
     */
    public boolean isActive() {
        // Eğer ceza affedilmişse, hiçbir zaman aktif değildir.
        if (this.pardoned) {
            return false;
        }
        // Kalıcı ise her zaman aktiftir (affedilmediği sürece).
        if (isPermanent()) {
            return true;
        }
        // Süreli ise, bitiş zamanının şimdiki zamandan sonra olup olmadığını kontrol et.
        return getExpiry().isAfter(Instant.now());
    }

    public boolean isPermanent() {
        return this.duration == null || this.duration.isZero();
    }

    public Instant getExpiry() {
        if (isPermanent()) {
            return Instant.MAX;
        }
        return this.creationTime.plus(this.duration);
    }
}