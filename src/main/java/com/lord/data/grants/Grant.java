package com.lord.data.grants;

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
    private final Duration duration; // Süresiz ise Duration.ZERO olabilir

    public Grant(UUID granteeUuid, String rankName, @Nullable UUID issuerUuid, Duration duration) {
        this.uniqueId = UUID.randomUUID();
        this.granteeUuid = granteeUuid;
        this.rankName = rankName;
        this.issuerUuid = issuerUuid;
        this.creationTime = Instant.now();
        this.duration = duration;
    }

    public boolean isPermanent() {
        return this.duration.isZero();
    }

    public boolean isActive() {
        if (isPermanent()) {
            return true;
        }
        return getExpiry().isAfter(Instant.now());
    }

    public Instant getExpiry() {
        if (isPermanent()) {
            return Instant.MAX;
        }
        return this.creationTime.plus(this.duration);
    }
}