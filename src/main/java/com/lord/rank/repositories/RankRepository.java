package com.lord.rank.repositories;

import com.lord.rank.Rank;

import java.util.Optional;
import java.util.Set;

public interface RankRepository {

    Optional<Rank> findByName(String name);

    Set<Rank> getAllRanks();

    void save(Rank rank);

    void delete(String name);

}