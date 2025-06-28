package com.lord.utils;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@UtilityClass
public class TimeUtil {

    /**
     * Parses a string like "30d", "12h", "perm" into a Duration.
     * Returns an empty Optional if the format is not a valid duration.
     * Returns Optional of Duration.ZERO for permanent.
     *
     * @param arg The string to parse.
     * @return An Optional containing the Duration.
     */
    public Optional<Duration> parseDuration(String arg) {
        if (arg == null) {
            return Optional.empty();
        }
        if (arg.equalsIgnoreCase("permanent") || arg.equalsIgnoreCase("perm")) {
            return Optional.of(Duration.ZERO);
        }

        try {
            char unit = arg.charAt(arg.length() - 1);
            long value = Long.parseLong(arg.substring(0, arg.length() - 1));

            return Optional.of(switch (unit) {
                case 's' -> Duration.ofSeconds(value);
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                case 'w' -> Duration.of(value, ChronoUnit.WEEKS);
                case 'M' -> Duration.of(value, ChronoUnit.MONTHS);
                case 'y' -> Duration.of(value, ChronoUnit.YEARS);
                default -> throw new IllegalArgumentException("Invalid time unit");
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "Permanent";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        String result = sb.toString().trim();
        return result.isEmpty() ? "Permanent" : result;
    }
}
