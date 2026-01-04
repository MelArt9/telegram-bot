package ru.melnikov.telegrambot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Единая точка конфигурации для всех настроек бота.
 * Все значения ТОЛЬКО из YAML.
 * ВСЯ логика расчета вынесена в сервисы!
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "telegram")
@Validated
public class BotSettingsConfig {

    // ==================== НАСТРОЙКИ БОТА ====================
    @NotNull
    private BotConfig bot = new BotConfig();

    @NotNull
    private AdminConfig admins = new AdminConfig();

    @NotNull
    private ReminderConfig reminders = new ReminderConfig();

    // ==================== ВЛОЖЕННЫЕ КЛАССЫ ====================

    @Data
    @Validated
    public static class BotConfig {
        @NotBlank
        private String token;

        @NotBlank
        private String username;

        @NotNull
        private Boolean enabled = true;

        public boolean isValid() {
            return token != null && !token.trim().isEmpty() &&
                    username != null && !username.trim().isEmpty();
        }
    }

    @Data
    @Validated
    public static class AdminConfig {
        @NotNull
        private java.util.List<String> usernames = new java.util.ArrayList<>();

        @NotNull
        private java.util.List<Long> userIds = new java.util.ArrayList<>();

        public boolean isAdminByUsername(String username) {
            if (username == null || username.isEmpty()) return false;
            String cleanUsername = username.startsWith("@") ? username.substring(1) : username;
            return usernames.stream().anyMatch(admin -> admin.equalsIgnoreCase(cleanUsername));
        }

        public boolean isAdminByUserId(Long userId) {
            return userId != null && userIds.contains(userId);
        }

        public boolean isAdmin(String username, Long userId) {
            return isAdminByUsername(username) || isAdminByUserId(userId);
        }
    }

    @Data
    @Validated
    public static class ReminderConfig {
        @NotNull
        private ScheduleConfig schedule = new ScheduleConfig();

        @NotNull
        private DeadlineConfig deadlines = new DeadlineConfig();

        @NotNull
        private BeforeClassConfig beforeClass = new BeforeClassConfig();

        @NotNull
        private WeekTypeConfig weekType = new WeekTypeConfig();

        @NotNull
        private SchedulerConfig scheduler = new SchedulerConfig();

        @Data
        @Validated
        public static class ScheduleConfig {
            @NotNull
            private Boolean enabled;

            @NotBlank
            @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Время должно быть в формате HH:mm")
            private String time;

            @NotBlank
            @Pattern(regexp = "^[01]{7}$", message = "Дни должны быть 7 символов (0 или 1)")
            private String days; // Пн-Вс, 1-включено, 0-выключено

            public LocalTime getTimeAsLocalTime() {
                return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            }

            public String getDaysDescription() {
                String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    if (days.charAt(i) == '1') {
                        result.append(dayNames[i]).append(", ");
                    }
                }
                if (result.length() > 0) {
                    result.setLength(result.length() - 2);
                }
                return result.toString();
            }
        }

        @Data
        @Validated
        public static class DeadlineConfig {
            @NotNull
            private Boolean enabled;

            @NotBlank
            @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Время должно быть в формате HH:mm")
            private String time;

            @NotBlank
            @Pattern(regexp = "^[01]{7}$", message = "Дни должны быть 7 символов (0 или 1)")
            private String days;

            public LocalTime getTimeAsLocalTime() {
                return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            }

            public String getDaysDescription() {
                String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    if (days.charAt(i) == '1') {
                        result.append(dayNames[i]).append(", ");
                    }
                }
                if (result.length() > 0) {
                    result.setLength(result.length() - 2);
                }
                return result.toString();
            }
        }

        @Data
        @Validated
        public static class BeforeClassConfig {
            @NotNull
            private Boolean enabled;

            @NotNull
            @Min(1)
            @Max(60)
            private Integer minutes;
        }

        @Data
        @Validated
        public static class WeekTypeConfig {
            @NotBlank
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Дата должна быть в формате yyyy-MM-dd")
            private String referenceDate;

            @NotBlank
            @Pattern(regexp = "even|odd", message = "Тип недели должен быть 'even' или 'odd'")
            private String referenceWeekType;

            public LocalDate getReferenceDateAsLocalDate() {
                return LocalDate.parse(referenceDate);
            }

            public String getWeekTypeDisplayName(String weekType) {
                if (weekType == null) return "ВСЕ";
                return "even".equals(weekType) ? "ЗНАМЕНАТЕЛЬ" : "ЧИСЛИТЕЛЬ";
            }

            public String getWeekTypeEmoji(String weekType) {
                if (weekType == null) return "🔄";
                return "even".equals(weekType) ? "2️⃣" : "1️⃣";
            }
        }

        @Data
        @Validated
        public static class SchedulerConfig {
            @NotNull
            private Boolean enabled;

            @NotBlank
            private String checkInterval;
        }
    }
}