package com.lord.repositories.impl;

import com.lord.data.grants.Grant;
import com.lord.repositories.GrantRepository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryGrantRepository implements GrantRepository {

    private final Map<UUID, Grant> grantsById = new ConcurrentHashMap<>();

    private final Map<UUID, Set<Grant>> grantsByPlayer = new ConcurrentHashMap<>();

    @Override
    public Optional<Grant> findById(UUID grantId) {
        return Optional.ofNullable(this.grantsById.get(grantId));
    }

    @Override
    public Set<Grant> findByPlayer(UUID playerUuid) {
        return Collections.unmodifiableSet(this.grantsByPlayer.getOrDefault(playerUuid, Collections.emptySet()));
    }

    @Override
    public void save(Grant grant) {
        this.grantsById.put(grant.getUniqueId(), grant);

        // Şimdi oyuncu indeksini güncelle.
        // computeIfAbsent: Eğer oyuncu için bir Set yoksa, yeni bir tane oluşturur.
        Set<Grant> playerGrants = this.grantsByPlayer.computeIfAbsent(grant.getGranteeUuid(), k -> ConcurrentHashMap.newKeySet());

        playerGrants.remove(grant);
        playerGrants.add(grant);
    }

    @Override
    public void delete(Grant grant) {
        this.grantsById.remove(grant.getUniqueId());

        Set<Grant> playerGrants = this.grantsByPlayer.get(grant.getGranteeUuid());
        if (playerGrants != null) {
            playerGrants.remove(grant);

            if (playerGrants.isEmpty()) {
                this.grantsByPlayer.remove(grant.getGranteeUuid());
            }
        }
    }
}