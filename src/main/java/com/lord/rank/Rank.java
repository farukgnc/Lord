package com.lord.rank;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
@ToString
public class Rank {

    private final String name;

    @Setter
    private String prefix;

    @Setter
    private String suffix;

    @Setter
    private int priority;

    private final Set<String> permissions = new HashSet<>();

    private final Set<String> parentRankNames = new HashSet<>();

}