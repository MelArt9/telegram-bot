package ru.melnikov.telegrambot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@Component
@ConfigurationProperties(prefix = "telegram.reminders")
public class ReminderConfig {

    private ScheduleConfig schedule = new ScheduleConfig();
    private DeadlinesConfig deadlines = new DeadlinesConfig();
    private BeforeClassConfig beforeClass = new BeforeClassConfig();
    private WeekTypeConfig weekType = new WeekTypeConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();

    @Data
    public static class ScheduleConfig {
        private boolean enabled = true;
        private String time = "08:00";
        private String days = "1111100"; // Пн-Пт

        public LocalTime getTimeAsLocalTime() {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        }
    }

    @Data
    public static class DeadlinesConfig {
        private boolean enabled = true;
        private String time = "10:00";
        private String days = "0101010"; // Вт, Чт, Пт

        public LocalTime getTimeAsLocalTime() {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        }
    }

    @Data
    public static class BeforeClassConfig {
        private boolean enabled = true;
        private int minutes = 15;
    }

    @Data
    public static class WeekTypeConfig {
        private String referenceDate = "2025-12-29";
        private String referenceWeekType = "even"; // "even" или "odd"

        public LocalDate getReferenceDateAsLocalDate() {
            return LocalDate.parse(referenceDate);
        }
    }

    @Data
    public static class SchedulerConfig {
        private boolean enabled = true;
        private String checkInterval = "0 * * * * *"; // Каждую минуту
    }
}