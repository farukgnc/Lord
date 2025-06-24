package com.lord.utils;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class TimeUtil {

    public Duration parseDuration(String arg) {
        if (arg == null || arg.equalsIgnoreCase("permanent") || arg.equalsIgnoreCase("perm")) {
            return Duration.ZERO;
        }

        try {
            long value = Long.parseLong(arg.substring(0, arg.length() - 1));
            char unit = arg.charAt(arg.length() - 1);

            return switch (unit) {
                case 's' -> Duration.ofSeconds(value);
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                case 'w' -> Duration.of(value, ChronoUnit.WEEKS);
                case 'M' -> Duration.of(value, ChronoUnit.MONTHS);
                case 'y' -> Duration.of(value, ChronoUnit.YEARS);
                default -> Duration.ZERO;
            };
        } catch (Exception e) {
            return Duration.ZERO;
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

        // Eğer çok küçük bir süre ise (örn: sadece saniye), sondaki boşluğu kaldır.
        String result = sb.toString().trim();
        return result.isEmpty() ? "Permanent" : result;
    }
}