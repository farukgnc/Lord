package com.lord.grant.repositories;

import com.lord.grant.Grant;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GrantRepository {

    Optional<Grant> findById(UUID grantId);

    Set<Grant> findByPlayer(UUID playerUuid);

    void save(Grant grant);

    void delete(Grant grant);

}