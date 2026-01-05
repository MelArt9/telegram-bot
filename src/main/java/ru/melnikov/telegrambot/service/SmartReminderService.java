package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.ScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartReminderService {

    // ====== ВСЕ НАСТРОЙКИ ТОЛЬКО ИЗ YML ======
    private final BotSettingsConfig settingsConfig;

    // ====== СЕРВИСЫ ДЛЯ ЛОГИКИ ======
    private final ReminderMessageService reminderMessageService;
    private final WeekTypeService weekTypeService;
    private final BotChatService botChatService;
    private final ScheduleRepository scheduleRepository;

    // ====== КОНСТАНТЫ И ФОРМАТТЕРЫ ======
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    // ====== НАСТРОЙКИ ИЗ YML (кешируем для производительности) ======

    /**
     * Настройки расписания ТОЛЬКО из YML
     */
    private BotSettingsConfig.ReminderConfig.ScheduleConfig getScheduleConfig() {
        return settingsConfig.getReminders().getSchedule();
    }

    /**
     * Настройки дедлайнов ТОЛЬКО из YML
     */
    private BotSettingsConfig.ReminderConfig.DeadlineConfig getDeadlineConfig() {
        return settingsConfig.getReminders().getDeadlines();
    }

    /**
     * Настройки напоминаний перед парой ТОЛЬКО из YML
     */
    private BotSettingsConfig.ReminderConfig.BeforeClassConfig getBeforeClassConfig() {
        return settingsConfig.getReminders().getBeforeClass();
    }

    /**
     * Настройки шедулера ТОЛЬКО из YML
     */
    private BotSettingsConfig.ReminderConfig.SchedulerConfig getSchedulerConfig() {
        return settingsConfig.getReminders().getScheduler();
    }

    // ====== ОСНОВНЫЕ МЕТОДЫ РАСПИСАНИЯ ======

    /**
     * Ежедневная проверка и отправка расписания по времени из YML
     * Комментируем или удаляем дублирующиеся методы
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndSendScheduleReminders() {
        // Оставляем только отправку расписания, НЕ напоминаний перед парой
        try {
            BotSettingsConfig.ReminderConfig.ScheduleConfig config = getScheduleConfig();

            if (!config.getEnabled()) {
                return;
            }

            LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
            LocalTime sendTime = config.getTimeAsLocalTime();

            if (currentTime.equals(sendTime) && shouldSendToday(config.getDays(), "расписание")) {
                log.info("📅 Время отправки расписания: {}", sendTime.format(TIME_FORMATTER));
                sendScheduleToGroups();
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки расписания: {}", e.getMessage(), e);
        }
    }

    /**
     * Еженедельная отправка дедлайнов по времени из YML
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndSendDeadlineReminders() {
        // Оставляем только отправку дедлайнов
        try {
            BotSettingsConfig.ReminderConfig.DeadlineConfig config = getDeadlineConfig();

            if (!config.getEnabled()) {
                return;
            }

            LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
            LocalTime sendTime = config.getTimeAsLocalTime();

            if (currentTime.equals(sendTime) && shouldSendToday(config.getDays(), "дедлайны")) {
                log.info("⏰ Время отправки дедлайнов: {}", sendTime.format(TIME_FORMATTER));
                sendDeadlinesToGroups();
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки дедлайнов: {}", e.getMessage(), e);
        }
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======

    /**
     * Получает расписание на сегодня с учетом типа недели из YML
     */
    private List<Schedule> getTodaySchedules() {
        try {
            int todayDayNumber = LocalDate.now().getDayOfWeek().getValue();
            String currentWeekType = weekTypeService.getCurrentWeekType();

            // Получаем все пары на сегодня из БД
            List<Schedule> allSchedules = scheduleRepository.findByDayOfWeek(todayDayNumber);

            // Фильтруем по типу недели из YML (через WeekTypeService)
            return allSchedules.stream()
                    .filter(schedule -> {
                        String scheduleWeekType = schedule.getWeekType() != null ?
                                schedule.getWeekType() : "all";
                        return scheduleWeekType.equals(currentWeekType) ||
                                scheduleWeekType.equals("all");
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Ошибка получения расписания на сегодня: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Отправляет напоминание о предстоящей паре
     */
    private void sendBeforeClassReminder(Schedule schedule, int minutesBefore) {
        try {
            Long chatId = getDefaultGroupChatId();
            if (chatId == null) {
                log.warn("⚠️ Не найден групповой чат для отправки напоминания");
                return;
            }

            String dayName = DayOfWeek.of(schedule.getDayOfWeek())
                    .getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            String currentWeekType = weekTypeService.getCurrentWeekType();
            String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);
            String weekTypeName = weekTypeService.getWeekTypeDisplayName(currentWeekType);

            String timeRange = String.format("%s-%s",
                    schedule.getTimeStart().format(TIME_FORMATTER),
                    schedule.getTimeEnd().format(TIME_FORMATTER));

            // Формируем сообщение из настроек YML
            String message = String.format("""
                    🔔 *НАПОМИНАНИЕ О ПРЕДСТОЯЩЕЙ ПАРЕ*
                    
                    📅 *%s* | %s %s
                    ⏰ *До начала осталось:* %d минут
                    
                    📖 *Предмет:* %s
                    👨‍🏫 *Преподаватель:* %s
                    📍 *Место:* %s
                    🕐 *Время:* %s
                    
                    🚀 *Удачной пары!*
                    """,
                    dayName, weekTypeEmoji, weekTypeName,
                    minutesBefore,
                    schedule.getSubject(),
                    schedule.getTeacher() != null ? schedule.getTeacher() : "не указан",
                    schedule.getIsOnline() != null && schedule.getIsOnline() ?
                            "💻 Онлайн" : (schedule.getLocation() != null ? schedule.getLocation() : "не указано"),
                    timeRange);

            // Отправляем с учетом темы из настроек чата
            reminderMessageService.sendMessageToChat(chatId, null, message, true);

            log.info("✅ Напоминание отправлено: '{}' за {} минут",
                    schedule.getSubject(), minutesBefore);

        } catch (Exception e) {
            log.error("❌ Ошибка отправки напоминания: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет расписание во все активные группы
     */
    private void sendScheduleToGroups() {
        List<BotChat> activeGroups = getActiveGroups();

        if (activeGroups.isEmpty()) {
            log.warn("⚠️ Нет активных групп для отправки расписания");
            return;
        }

        log.info("📤 Отправка расписания в {} групп", activeGroups.size());

        activeGroups.forEach(group -> {
            try {
                reminderMessageService.sendScheduleToChat(group.getChatId());
                Thread.sleep(100); // Пауза между отправками
            } catch (Exception e) {
                log.error("❌ Ошибка отправки в чат {}: {}", group.getChatId(), e.getMessage());
            }
        });
    }

    /**
     * Отправляет дедлайны во все активные группы
     */
    private void sendDeadlinesToGroups() {
        List<BotChat> activeGroups = getActiveGroups();

        if (activeGroups.isEmpty()) {
            log.warn("⚠️ Нет активных групп для отправки дедлайнов");
            return;
        }

        log.info("📤 Отправка дедлайнов в {} групп", activeGroups.size());

        activeGroups.forEach(group -> {
            try {
                reminderMessageService.sendDeadlinesToChat(group.getChatId());
                Thread.sleep(100); // Пауза между отправками
            } catch (Exception e) {
                log.error("❌ Ошибка отправки в чат {}: {}", group.getChatId(), e.getMessage());
            }
        });
    }

    /**
     * Получает все активные группы из БД
     */
    private List<BotChat> getActiveGroups() {
        return botChatService.findAllActiveChats().stream()
                .filter(this::isGroupChat)
                .collect(Collectors.toList());
    }

    /**
     * Получает ID группового чата по умолчанию
     */
    private Long getDefaultGroupChatId() {
        return getActiveGroups().stream()
                .findFirst()
                .map(BotChat::getChatId)
                .orElse(null);
    }

    /**
     * Проверяет, нужно ли отправлять сегодня по паттерну из YML
     */
    private boolean shouldSendToday(String daysPattern, String reminderType) {
        if (daysPattern == null || daysPattern.length() != 7) {
            log.warn("⚠️ Некорректный паттерн дней для {} в YML: {}", reminderType, daysPattern);
            return false;
        }

        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;

        if (todayIndex < 0 || todayIndex >= daysPattern.length()) {
            return false;
        }

        boolean shouldSend = daysPattern.charAt(todayIndex) == '1';

        if (!shouldSend) {
            log.debug("⏸️ Сегодня не день для отправки {} (паттерн из YML: {})",
                    reminderType, daysPattern);
        }

        return shouldSend;
    }

    /**
     * Проверяет, является ли чат группой
     */
    private boolean isGroupChat(BotChat chat) {
        if (chat == null || chat.getChatType() == null) {
            return false;
        }

        String chatType = chat.getChatType().toLowerCase();
        return chatType.contains("group") || chatType.contains("supergroup");
    }

    // ====== ТЕСТОВЫЕ И ОТЛАДОЧНЫЕ МЕТОДЫ ======

    /**
     * Получает полную информацию о настройках из YML
     */
    public Map<String, Object> getYmlSettingsInfo() {
        Map<String, Object> info = new HashMap<>();

        // Расписание
        BotSettingsConfig.ReminderConfig.ScheduleConfig scheduleConfig = getScheduleConfig();
        info.put("schedule", Map.of(
                "enabled", scheduleConfig.getEnabled(),
                "time", scheduleConfig.getTime(),
                "days", scheduleConfig.getDays(),
                "daysDescription", scheduleConfig.getDaysDescription(),
                "configSource", "YML (telegram.reminders.schedule)"
        ));

        // Дедлайны
        BotSettingsConfig.ReminderConfig.DeadlineConfig deadlineConfig = getDeadlineConfig();
        info.put("deadlines", Map.of(
                "enabled", deadlineConfig.getEnabled(),
                "time", deadlineConfig.getTime(),
                "days", deadlineConfig.getDays(),
                "daysDescription", deadlineConfig.getDaysDescription(),
                "configSource", "YML (telegram.reminders.deadlines)"
        ));

        // Перед парой
        BotSettingsConfig.ReminderConfig.BeforeClassConfig beforeClassConfig = getBeforeClassConfig();
        info.put("beforeClass", Map.of(
                "enabled", beforeClassConfig.getEnabled(),
                "minutes", beforeClassConfig.getMinutes(),
                "configSource", "YML (telegram.reminders.before-class)"
        ));

        // Шедулер
        BotSettingsConfig.ReminderConfig.SchedulerConfig schedulerConfig = getSchedulerConfig();
        info.put("scheduler", Map.of(
                "enabled", schedulerConfig.getEnabled(),
                "checkInterval", schedulerConfig.getCheckInterval(),
                "configSource", "YML (telegram.reminders.scheduler)"
        ));

        // Недели
        BotSettingsConfig.ReminderConfig.WeekTypeConfig weekTypeConfig =
                settingsConfig.getReminders().getWeekType();
        info.put("weekType", Map.of(
                "referenceDate", weekTypeConfig.getReferenceDate(),
                "referenceWeekType", weekTypeConfig.getReferenceWeekType(),
                "configSource", "YML (telegram.reminders.week-type)"
        ));

        info.put("currentTime", LocalTime.now().format(TIME_FORMATTER));
        info.put("currentDate", LocalDate.now().toString());
        info.put("todayWillSendSchedule", shouldSendToday(scheduleConfig.getDays(), "расписание"));
        info.put("todayWillSendDeadlines", shouldSendToday(deadlineConfig.getDays(), "дедлайны"));

        return info;
    }

    /**
     * Тестовая отправка расписания сейчас (для отладки)
     */
    public void sendTestScheduleNow() {
        if (!getScheduleConfig().getEnabled()) {
            log.warn("⚠️ Расписание отключено в YML, тестовая отправка невозможна");
            return;
        }

        try {
            log.info("🧪 Тестовая отправка расписания (время из YML: {})",
                    getScheduleConfig().getTime());
            sendScheduleToGroups();
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки: {}", e.getMessage(), e);
        }
    }

    /**
     * Тестовая отправка дедлайнов сейчас (для отладки)
     */
    public void sendTestDeadlinesNow() {
        if (!getDeadlineConfig().getEnabled()) {
            log.warn("⚠️ Дедлайны отключены в YML, тестовая отправка невозможна");
            return;
        }

        try {
            log.info("🧪 Тестовая отправка дедлайнов (время из YML: {})",
                    getDeadlineConfig().getTime());
            sendDeadlinesToGroups();
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверка работоспособности сервиса
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Проверка конфигурации из YML
            health.put("scheduleConfigValid", getScheduleConfig() != null);
            health.put("deadlineConfigValid", getDeadlineConfig() != null);
            health.put("beforeClassConfigValid", getBeforeClassConfig() != null);
            health.put("schedulerConfigValid", getSchedulerConfig() != null);

            // Проверка зависимостей
            health.put("reminderMessageService", reminderMessageService != null ? "OK" : "ERROR");
            health.put("weekTypeService", weekTypeService != null ? "OK" : "ERROR");
            health.put("botChatService", botChatService != null ? "OK" : "ERROR");
            health.put("scheduleRepository", scheduleRepository != null ? "OK" : "ERROR");

            // Статус
            health.put("status", "HEALTHY");
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("configSource", "YML ONLY");
            health.put("activeGroupsCount", getActiveGroups().size());

        } catch (Exception e) {
            health.put("status", "UNHEALTHY");
            health.put("error", e.getMessage());
            health.put("errorType", e.getClass().getName());
        }

        return health;
    }
}