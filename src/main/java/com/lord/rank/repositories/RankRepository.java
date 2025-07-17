package com.lord.rank.repositories;

import com.lord.rank.Rank;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RankRepository {

    Optional<Rank> findByName(String name);

    Set<Rank> getAllRanks();

    CompletableFuture<Set<Rank>> loadAllRanks();

    CompletableFuture<Boolean> save(Rank rank);

    CompletableFuture<Boolean> delete(String name);

    CompletableFuture<Boolean> isEmpty();

}
