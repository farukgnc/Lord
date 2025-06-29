package com.lord.grant;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public final class Grant {

    private final UUID uniqueId;
    private final UUID granteeUuid;
    private final String rankName;

    private final UUID issuerUuid; // Null olabilir (örn: konsol tarafından verildi)
    private final Instant creationTime;
    private final Duration duration;

    /**
     * Yeni bir grant oluşturmak için kullanılır. ID ve oluşturulma zamanını otomatik atar.
     */
    public Grant(UUID granteeUuid, String rankName, @Nullable UUID issuerUuid, Duration duration) {
        this.uniqueId = UUID.randomUUID();
        this.granteeUuid = granteeUuid;
        this.rankName = rankName;
        this.issuerUuid = issuerUuid;
        this.creationTime = Instant.now();
        this.duration = duration;
    }

    /**
     * Veritabanından bir kaydı yeniden oluşturmak için kullanılır.
     */
    public Grant(UUID uniqueId, UUID granteeUuid, String rankName, @Nullable UUID issuerUuid, Instant creationTime, Duration duration) {
        this.uniqueId = uniqueId;
        this.granteeUuid = granteeUuid;
        this.rankName = rankName;
        this.issuerUuid = issuerUuid;
        this.creationTime = creationTime;
        this.duration = duration;
    }

    public boolean isPermanent() {
        return this.duration == null || this.duration.isZero();
    }

    public boolean isActive() {
        if (isPermanent()) {
            return true;
        }
        return getExpiry().isAfter(Instant.now());
    }

    public Instant getExpiry() {
        if (isPermanent()) {
            // Pratikte sonsuz bir tarih
            return Instant.MAX;
        }
        return this.creationTime.plus(this.duration);
    }
}