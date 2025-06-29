package com.lord.grant.repositories;

import com.lord.grant.Grant;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GrantRepository {

    CompletableFuture<Optional<Grant>> findById(UUID grantId);

    CompletableFuture<Set<Grant>> findByPlayer(UUID playerUuid);

    CompletableFuture<Void> save(Grant grant);

    CompletableFuture<Void> delete(Grant grant);

}