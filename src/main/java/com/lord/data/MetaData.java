package com.lord.data;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public final class MetaData {

    /**
     * The final prefix for the player, determined by rank weights.
     */
    @Nullable
    private final String prefix;

    /**
     * The final suffix for the player, determined by rank weights.
     */
    @Nullable
    private final String suffix;

    /**
     * The name of the player's primary rank, determined by rank weights.
     */
    @Nullable
    private final String primaryRank;

    public MetaData(@Nullable String prefix, @Nullable String suffix, @Nullable String primaryRank) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.primaryRank = primaryRank;
    }
}