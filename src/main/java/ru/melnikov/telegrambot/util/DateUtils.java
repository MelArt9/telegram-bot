package ru.melnikov.telegrambot.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

public final class DateUtils {

    private DateUtils() {
    }

    public static boolean isOddWeek(LocalDate date) {
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return weekNumber % 2 != 0;
    }

    public static String weekTypeForDate(LocalDate date) {
        return isOddWeek(date) ? "odd" : "even";
    }

    public static DayOfWeek toDayOfWeek(int day) {
        if (day < 1 || day > 7) {
            return null;
        }
        return DayOfWeek.of(day);
    }
}
