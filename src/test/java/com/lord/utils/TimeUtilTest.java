package com.lord.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void shouldParsePermanentKeywords() {
        assertThat(TimeUtil.parseDuration("perm")).contains(Duration.ZERO);
        assertThat(TimeUtil.parseDuration("permanent")).contains(Duration.ZERO);
    }

    @Test
    void shouldParseCommonDurationUnits() {
        assertThat(TimeUtil.parseDuration("30s")).contains(Duration.ofSeconds(30));
        assertThat(TimeUtil.parseDuration("15m")).contains(Duration.ofMinutes(15));
        assertThat(TimeUtil.parseDuration("12h")).contains(Duration.ofHours(12));
        assertThat(TimeUtil.parseDuration("7d")).contains(Duration.ofDays(7));
    }

    @Test
    void shouldReturnEmptyForInvalidDuration() {
        assertThat(TimeUtil.parseDuration("abc")).isEmpty();
        assertThat(TimeUtil.parseDuration("10x")).isEmpty();
        assertThat(TimeUtil.parseDuration(null)).isEmpty();
    }

    @Test
    void shouldFormatDurationHumanReadable() {
        Duration duration = Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4);

        assertThat(TimeUtil.formatDuration(duration)).isEqualTo("1d 2h 3m 4s");
        assertThat(TimeUtil.formatDuration(Duration.ZERO)).isEqualTo("Permanent");
        assertThat(TimeUtil.formatDuration(null)).isEqualTo("Permanent");
    }
}
