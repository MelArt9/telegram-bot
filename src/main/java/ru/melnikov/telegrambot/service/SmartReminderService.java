package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.melnikov.telegrambot.bot.CommandService;
import ru.melnikov.telegrambot.config.ReminderConfig;
import ru.melnikov.telegrambot.model.*;
import ru.melnikov.telegrambot.repository.BotChatRepository;
import ru.melnikov.telegrambot.repository.ReminderRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartReminderService {

    private final ReminderRepository reminderRepository;
    private final BotChatRepository botChatRepository;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final UserService userService;
    private final TelegramMessageSender telegramMessageSender;
    private final ReminderConfig reminderConfig;
    private final CommandService commandService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    /**
     * Проверка напоминаний каждую минуту
     */
    @Scheduled(cron = "${telegram.reminders.scheduler.check-interval:0 * * * * *}")
    public void checkReminders() {
        if (!reminderConfig.getScheduler().isEnabled()) {
            return;
        }

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        // 1. Проверяем напоминания по времени
        List<Reminder> timeReminders = reminderRepository.findActiveRemindersByTime(now);
        for (Reminder reminder : timeReminders) {
            if (shouldSendToday(reminder)) {
                sendSmartReminder(reminder);
                reminder.setLastSentAt(LocalDateTime.now());
                reminderRepository.save(reminder);
            }
        }

        // 2. Проверяем напоминания за N минут до пар (каждую минуту)
        checkScheduleRemindersBeforeClass();
    }

    /**
     * Проверка напоминаний за N минут до пар
     */
    private void checkScheduleRemindersBeforeClass() {
        // Получаем все активные чаты
        List<BotChat> activeChats = botChatRepository.findAllActiveChats();
        LocalDateTime now = LocalDateTime.now();

        for (BotChat chat : activeChats) {
            try {
                // Получаем настройки
                Map<String, Object> settings = chat.getSettings();

                // Проверяем, включены ли уведомления о расписании
                if (settings != null && Boolean.TRUE.equals(settings.get("schedule_notifications"))) {
                    Integer minutesBefore = getSettingAsInt(settings, "reminder_before_class", 15);

                    // Получаем расписание на сегодня
                    List<Schedule> todaySchedule = scheduleService.findEntitiesToday();

                    // Проверяем каждую пару
                    for (Schedule schedule : todaySchedule) {
                        LocalTime classStart = schedule.getTimeStart();
                        LocalTime reminderTime = classStart.minusMinutes(minutesBefore);

                        // Если текущее время совпадает с временем напоминания
                        if (now.toLocalTime().withSecond(0).withNano(0).equals(reminderTime)) {
                            sendBeforeClassReminder(chat.getChatId(), schedule, minutesBefore);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка проверки напоминаний для чата {}: {}", chat.getChatId(), e.getMessage());
            }
        }
    }

    /**
     * Генерация умного сообщения для напоминания
     */
    private String generateSmartMessage(Reminder reminder) {
        return switch (reminder.getReminderType()) {
            case "SCHEDULE_TODAY" -> generateTodayScheduleMessage(reminder.getChatId());
            case "DEADLINE_WEEKLY" -> generateWeeklyDeadlinesMessage(reminder.getChatId());
            default -> "🔔 Напоминание";
        };
    }

    /**
     * Генерация сообщения с расписанием на сегодня
     */
    private String generateTodayScheduleMessage(Long chatId) {
        List<Schedule> scheduleList = scheduleService.findEntitiesToday();

        if (scheduleList.isEmpty()) {
            DayOfWeek today = LocalDate.now().getDayOfWeek();
            String dayName = today.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);

            return String.format("""
                    📅 *РАСПИСАНИЕ НА СЕГОДНЯ*
                    *%s*
                    
                    🎉 *Сегодня занятий нет!*
                    
                    💡 *Можно заняться:* • Самостоятельной подготовкой • Отдыхом
                    """,
                    dayName.substring(0, 1).toUpperCase() + dayName.substring(1));
        }

        // Фильтруем расписание для текущей недели
        String currentWeekType = getCurrentWeekType();
        List<Schedule> filteredSchedule = scheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .toList();

        return formatScheduleForReminder(filteredSchedule, "сегодня");
    }

    /**
     * Генерация сообщения о дедлайнах на неделю
     */
    private String generateWeeklyDeadlinesMessage(Long chatId) {
        List<Deadline> allDeadlines = deadlineService.findAllDeadlinesSorted();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekEnd = now.plusDays(7);

        // Фильтруем дедлайны на ближайшую неделю
        List<Deadline> weeklyDeadlines = allDeadlines.stream()
                .filter(d -> {
                    LocalDateTime deadline = d.getDeadlineAt();
                    return !deadline.isBefore(now) && deadline.isBefore(weekEnd);
                })
                .sorted(Comparator.comparing(Deadline::getDeadlineAt))
                .toList();

        if (weeklyDeadlines.isEmpty()) {
            return """
                    ⏰ *ДЕДЛАЙНЫ НА НЕДЕЛЮ*
                    
                    🎉 *На этой неделе дедлайнов нет!*
                    
                    💡 *Можно заняться:* 
                     • Опережающей подготовкой 
                     • Повторением материала
                    """;
        }

        // Группируем дедлайны по дням
        Map<LocalDate, List<Deadline>> deadlinesByDay = weeklyDeadlines.stream()
                .collect(Collectors.groupingBy(d -> d.getDeadlineAt().toLocalDate()));

        StringBuilder message = new StringBuilder();
        message.append("⏰ *ДЕДЛАЙНЫ НА БЛИЖАЙШУЮ НЕДЕЛЮ*\n\n");

        // Сортируем дни
        List<LocalDate> sortedDays = new ArrayList<>(deadlinesByDay.keySet());
        Collections.sort(sortedDays);

        for (LocalDate day : sortedDays) {
            String dayName = day.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            message.append(String.format("📅 *%s (%s)*\n",
                    day.format(DateTimeFormatter.ofPattern("dd.MM")),
                    dayName.substring(0, 1).toUpperCase() + dayName.substring(1)));

            for (Deadline deadline : deadlinesByDay.get(day)) {
                long daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), day);

                String urgency;
                if (daysUntil == 0) urgency = "🔴 СЕГОДНЯ";
                else if (daysUntil <= 2) urgency = "🟡 СКОРО";
                else urgency = "🟢 НА ЭТОЙ НЕДЕЛЕ";

                message.append(String.format("   %s *%s*\n", urgency, deadline.getTitle()))
                        .append(String.format("      ⏰ %s\n",
                                deadline.getDeadlineAt().format(DATETIME_FORMATTER)))
                        .append(String.format("      📝 %s\n",
                                deadline.getDescription() != null && !deadline.getDescription().isBlank() ?
                                        deadline.getDescription() : "Без описания"));

                if (deadline.getLinkUrl() != null && !deadline.getLinkUrl().isBlank()) {
                    String linkText = deadline.getLinkText() != null && !deadline.getLinkText().isBlank() ?
                            deadline.getLinkText() : "Ссылка";
                    message.append(String.format("      🔗 [%s](%s)\n", linkText, deadline.getLinkUrl()));
                }

                message.append("\n");
            }

            message.append("\n");
        }

        return message.toString();
    }

    /**
     * Форматирование расписания для напоминания
     */
    private String formatScheduleForReminder(List<Schedule> scheduleList, String context) {
        if (scheduleList.isEmpty()) {
            return String.format("📭 *На %s пар нет*", context);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 *РАСПИСАНИЕ НА ").append(context.toUpperCase()).append("*\n\n");

        for (int i = 0; i < scheduleList.size(); i++) {
            Schedule s = scheduleList.get(i);
            String timeRange = String.format("%s-%s",
                    s.getTimeStart().format(TIME_FORMATTER),
                    s.getTimeEnd().format(TIME_FORMATTER));

            String weekTypeEmoji = getWeekTypeEmoji(s.getWeekType());
            Boolean isOnline = s.getIsOnline();
            String onlineEmoji = (isOnline != null && isOnline) ? "💻" : "🏫";
            String locationInfo = (isOnline != null && isOnline) ?
                    "💻 Онлайн" : (s.getLocation() != null ? s.getLocation() : "Аудитория не указана");

            sb.append(String.format("%d. %s %s\n", i + 1, weekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s\n", s.getSubject()))
                    .append(String.format("   👨‍🏫 %s\n",
                            s.getTeacher() != null ? s.getTeacher() : "Преподаватель не указан"))
                    .append(String.format("   📍 %s\n", locationInfo))
                    .append("\n");
        }

        // Добавляем статистику
        int onlinePairs = (int) scheduleList.stream()
                .filter(s -> s.getIsOnline() != null && s.getIsOnline())
                .count();

        sb.append(String.format("""
                📊 *Статистика дня:*
                📝 Всего пар: %d
                💻 Онлайн: %d
                🏫 Очных: %d
                """,
                scheduleList.size(),
                onlinePairs,
                scheduleList.size() - onlinePairs));

        return sb.toString();
    }

    /**
     * Форматирование сообщения перед парой
     */
    private String formatBeforeClassMessage(Schedule schedule, int minutesBefore) {
        String weekTypeEmoji = getWeekTypeEmoji(schedule.getWeekType());
        String onlineEmoji = (schedule.getIsOnline() != null && schedule.getIsOnline()) ? "💻" : "🏫";

        return String.format("""
                🔔 *НАПОМИНАНИЕ О ПАРЕ*
                
                %s %s *Через %d минут начинается пара*
                
                📖 *Предмет:* %s
                👨‍🏫 *Преподаватель:* %s
                📍 *Место:* %s
                ⏰ *Время:* %s - %s
                
                ⚡ *Успейте подготовиться!*
                """,
                weekTypeEmoji, onlineEmoji, minutesBefore,
                schedule.getSubject(),
                schedule.getTeacher() != null ? schedule.getTeacher() : "Не указан",
                schedule.getIsOnline() != null && schedule.getIsOnline() ?
                        "💻 Онлайн" :
                        (schedule.getLocation() != null ? schedule.getLocation() : "Не указана"),
                schedule.getTimeStart().format(TIME_FORMATTER),
                schedule.getTimeEnd().format(TIME_FORMATTER));
    }

    /**
     * Получение текущего типа недели
     */
    private String getCurrentWeekType() {
        LocalDate today = LocalDate.now();
        LocalDate referenceDate = LocalDate.of(2024, 9, 2); // Начало учебного года
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(
                referenceDate.with(DayOfWeek.MONDAY),
                today.with(DayOfWeek.MONDAY)
        );
        return weeksBetween % 2 == 0 ? "even" : "odd";
    }

    /**
     * Получение эмодзи типа недели
     */
    private String getWeekTypeEmoji(String weekType) {
        if (weekType == null) return "🔄";
        return switch (weekType.toLowerCase()) {
            case "odd" -> "1️⃣";
            case "even" -> "2️⃣";
            case "all" -> "🔄";
            default -> "🔄";
        };
    }

    /**
     * Инициализация умных напоминаний для чата
     */
    @Transactional
    public void initializeSmartReminders(Long chatId, Long userId) {
        try {
            User user = userService.findById(userId);

            // 1. Проверяем, не созданы ли уже напоминания
            Optional<Reminder> existingSchedule = reminderRepository.findByChatIdAndType(chatId, "SCHEDULE_TODAY");
            Optional<Reminder> existingDeadline = reminderRepository.findByChatIdAndType(chatId, "DEADLINE_WEEKLY");

            // 2. Ежедневное расписание
            if (reminderConfig.getSchedule().isEnabled()) {
                Reminder scheduleReminder = Reminder.builder()
                        .chatId(chatId)
                        .reminderType("SCHEDULE_TODAY")
                        .scheduleTime(reminderConfig.getSchedule().getTimeAsLocalTime())
                        .daysOfWeek(reminderConfig.getSchedule().getDays())
                        .isActive(true)
                        .build();

                reminderRepository.save(scheduleReminder);
            }

            // 3. Еженедельные дедлайны
            if (reminderConfig.getDeadlines().isEnabled()) {
                // Создаем напоминание о дедлайнах
                Reminder deadlineReminder = Reminder.builder()
                        .chatId(chatId)
                        .reminderType("DEADLINE_WEEKLY")
                        .scheduleTime(reminderConfig.getDeadlines().getTimeAsLocalTime())
                        .daysOfWeek(reminderConfig.getDeadlines().getDays())
                        .isActive(true)
                        .build();

                reminderRepository.save(deadlineReminder);
            }

            log.info("Инициализированы умные напоминания для чата {}", chatId);
        } catch (Exception e) {
            log.error("Ошибка инициализации напоминаний для чата {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Получение списка напоминаний для чата
     */
    @Transactional(readOnly = true)
    public List<Reminder> getChatReminders(Long chatId) {
        return reminderRepository.findByChatIdAndIsActiveTrue(chatId);
    }

    /**
     * Включение/выключение напоминаний
     */
    @Transactional
    public void toggleReminderType(Long chatId, String reminderType, boolean active) {
        reminderRepository.findByChatIdAndType(chatId, reminderType).ifPresent(reminder -> {
            reminder.setIsActive(active);
            reminderRepository.save(reminder);
        });
    }

    /**
     * Обновление времени напоминаний
     */
    @Transactional
    public void updateReminderTime(Long chatId, String reminderType, LocalTime newTime) {
        reminderRepository.findByChatIdAndType(chatId, reminderType).ifPresent(reminder -> {
            reminder.setScheduleTime(newTime);
            reminderRepository.save(reminder);
        });
    }

    /**
     * Вспомогательный метод для получения настройки как Integer
     */
    private Integer getSettingAsInt(Map<String, Object> settings, String key, Integer defaultValue) {
        try {
            if (settings != null && settings.containsKey(key)) {
                Object value = settings.get(key);
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                } else if (value instanceof Boolean) {
                    return (Boolean) value ? 15 : 0; // Если булево значение, возвращаем 15 или 0
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось получить настройку {} как Integer", key);
        }
        return defaultValue;
    }

    /**
     * Проверка, нужно ли отправлять напоминание сегодня
     */
    private boolean shouldSendToday(Reminder reminder) {
        if (reminder.getDaysOfWeek() == null || reminder.getDaysOfWeek().length() != 7) {
            return true;
        }

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        int dayIndex = today.getValue() - 1;
        return reminder.getDaysOfWeek().charAt(dayIndex) == '1';
    }

    /**
     * Отправка интеллектуального напоминания
     */
    private void sendSmartReminder(Reminder reminder) {
        try {
            String message = generateSmartMessage(reminder);
            if (message != null && !message.trim().isEmpty()) {
                telegramMessageSender.sendMarkdownMessage(reminder.getChatId(), message);
                log.info("Отправлено умное напоминание {} в чат {}", reminder.getId(), reminder.getChatId());
            }
        } catch (Exception e) {
            log.error("Ошибка отправки напоминания в чат {}: {}", reminder.getChatId(), e.getMessage());
        }
    }

    /**
     * Отправка напоминания за N минут до пары
     */
    private void sendBeforeClassReminder(Long chatId, Schedule schedule, int minutesBefore) {
        try {
            String message = formatBeforeClassMessage(schedule, minutesBefore);
            telegramMessageSender.sendMarkdownMessage(chatId, message);
            log.info("Отправлено напоминание перед парой в чат {}", chatId);
        } catch (Exception e) {
            log.error("Ошибка отправки напоминания перед парой в чат {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Получение расписания на сегодня с учетом типа недели
     */
    private List<Schedule> getFilteredScheduleForToday() {
        List<Schedule> scheduleList = scheduleService.findEntitiesToday();
        String currentWeekType = commandService.getCurrentWeekType();

        // Фильтруем по типу недели (odd/even + all)
        return scheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .toList();
    }
}