package me.testikgm.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static String formatTicks(long ticks) {
        return formatDuration(ticks * 50);
    }

    public static String getCurrentTimeString() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    public static long msToTicks(long milliseconds) {
        return milliseconds / 50;
    }

    public static long ticksToMs(long ticks) {
        return ticks * 50;
    }
}

