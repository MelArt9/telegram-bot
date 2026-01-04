// Файл: /src/main/java/ru/melnikov/telegrambot/service/ReminderMessageService.java
package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.melnikov.telegrambot.bot.TelegramBot;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.BotChatRepository;
import ru.melnikov.telegrambot.repository.DeadlineRepository;
import ru.melnikov.telegrambot.repository.ScheduleRepository;
import ru.melnikov.telegrambot.util.DeadlineFormatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderMessageService {

    private final TelegramBot telegramBot;
    private final BotChatRepository botChatRepository;
    private final ScheduleRepository scheduleRepository;
    private final DeadlineRepository deadlineRepository;
    private final WeekTypeService weekTypeService;
    private final BotSettingsConfig settingsConfig;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    /**
     * Отправляет расписание на сегодня во все активные чаты
     */
    public void sendDailyScheduleToAllChats() {
        log.info("📅 Начинаю рассылку расписания на сегодня...");

        // Получаем все активные чаты с включенными уведомлениями о расписании
        List<BotChat> activeChats = botChatRepository.findChatsWithScheduleNotifications();

        if (activeChats.isEmpty()) {
            log.info("📭 Нет активных чатов с включенными уведомлениями о расписании");
            return;
        }

        log.info("🔔 Найдено {} чатов для рассылки расписания", activeChats.size());

        // Получаем расписание на сегодня
        String scheduleMessage = generateTodayScheduleMessage();

        for (BotChat chat : activeChats) {
            try {
                sendMessageToChat(chat.getChatId(), scheduleMessage);
                log.info("✅ Расписание отправлено в чат {}: {}", chat.getChatId(), chat.getTitle());
            } catch (Exception e) {
                log.error("❌ Ошибка отправки в чат {}: {}", chat.getChatId(), e.getMessage());
            }
        }

        log.info("✅ Рассылка расписания завершена");
    }

    /**
     * Отправляет дедлайны на неделю во все активные чаты
     */
    public void sendWeeklyDeadlinesToAllChats() {
        log.info("⏰ Начинаю рассылку дедлайнов на неделю...");

        // Получаем все активные чаты с включенными уведомлениями о дедлайнах
        List<BotChat> activeChats = botChatRepository.findChatsWithDeadlineNotifications();

        if (activeChats.isEmpty()) {
            log.info("📭 Нет активных чатов с включенными уведомлениями о дедлайнах");
            return;
        }

        log.info("🔔 Найдено {} чатов для рассылки дедлайнов", activeChats.size());

        // Получаем дедлайны на ближайшую неделю
        String deadlinesMessage = generateWeeklyDeadlinesMessage();

        for (BotChat chat : activeChats) {
            try {
                sendMessageToChat(chat.getChatId(), deadlinesMessage);
                log.info("✅ Дедлайны отправлены в чат {}: {}", chat.getChatId(), chat.getTitle());
            } catch (Exception e) {
                log.error("❌ Ошибка отправки в чат {}: {}", chat.getChatId(), e.getMessage());
            }
        }

        log.info("✅ Рассылка дедлайнов завершена");
    }

    /**
     * Отправляет напоминания перед парой
     */
    public void sendBeforeClassReminders() {
        log.info("⏳ Проверяю напоминания перед парой...");

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        // Получаем все активные чаты
        List<BotChat> activeChats = botChatRepository.findAllActiveChats();

        for (BotChat chat : activeChats) {
            try {
                // Получаем настройку времени напоминания для этого чата
                int reminderMinutes = getReminderBeforeClassMinutes(chat);

                if (reminderMinutes <= 0) {
                    continue; // Напоминания отключены для этого чата
                }

                // Генерируем напоминания для этого чата
                sendBeforeClassRemindersForChat(chat, now, reminderMinutes);

            } catch (Exception e) {
                log.error("❌ Ошибка проверки напоминаний для чата {}: {}", chat.getChatId(), e.getMessage());
            }
        }
    }

    /**
     * Генерирует сообщение с расписанием на сегодня
     */
    private String generateTodayScheduleMessage() {
        LocalDate today = LocalDate.now();
        String currentWeekType = weekTypeService.getCurrentWeekType();
        String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);
        String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);

        int dayNumber = today.getDayOfWeek().getValue();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

        // Получаем расписание на сегодня
        List<Schedule> allScheduleList = scheduleRepository.findByDayOfWeek(dayNumber);

        // Фильтруем по типу недели
        List<Schedule> filteredScheduleList = allScheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .collect(Collectors.toList());

        if (filteredScheduleList.isEmpty()) {
            return String.format("""
                📭 *Сегодня занятий нет!* 📭
                
                📅 *День:* %s
                🗓️ *Тип недели:* %s %s
                
                🎉 *Можно отдохнуть или заняться саморазвитием!*
                """,
                    dayName,
                    weekTypeEmoji, weekTypeDisplay);
        }

        // Форматируем расписание
        StringBuilder scheduleText = new StringBuilder();

        for (int i = 0; i < filteredScheduleList.size(); i++) {
            Schedule s = filteredScheduleList.get(i);
            String timeRange = String.format("%s-%s",
                    s.getTimeStart().format(TIME_FORMATTER),
                    s.getTimeEnd().format(TIME_FORMATTER));

            String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
            String pairWeekTypeEmoji = getWeekTypeEmoji(scheduleWeekType);
            String onlineEmoji = (s.getIsOnline() != null && s.getIsOnline()) ? "💻" : "🏫";

            scheduleText.append(String.format("%d. %s %s\n", i + 1, pairWeekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s\n", s.getSubject()));

            if (s.getTeacher() != null && !s.getTeacher().isBlank()) {
                scheduleText.append(String.format("   👨‍🏫 %s\n", s.getTeacher()));
            }

            if (s.getLocation() != null && !s.getLocation().isBlank()) {
                String location = (s.getIsOnline() != null && s.getIsOnline()) ?
                        "Онлайн" : s.getLocation();
                scheduleText.append(String.format("   📍 %s\n", location));
            }

            scheduleText.append("\n");
        }

        return String.format("""
            🔔 *ЕЖЕДНЕВНОЕ РАСПИСАНИЕ*
            
            📅 *День:* %s
            🗓️ *Тип недели:* %s %s
            
            %s
            📊 *Всего пар сегодня:* %d
            
            🚀 *Хорошего учебного дня!*
            """,
                dayName,
                weekTypeEmoji, weekTypeDisplay,
                scheduleText.toString(),
                filteredScheduleList.size());
    }

    /**
     * Генерирует сообщение с дедлайнами на неделю
     */
    private String generateWeeklyDeadlinesMessage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoWeeksLater = now.plusDays(14); // Увеличиваем до 2 недель

        // Получаем все дедлайны на ближайшие 2 недели
        List<Deadline> deadlines = deadlineRepository.findAll().stream()
                .filter(d -> {
                    LocalDateTime deadlineAt = d.getDeadlineAt();
                    return deadlineAt.isAfter(now) && deadlineAt.isBefore(twoWeeksLater);
                })
                .sorted(Comparator.comparing(Deadline::getDeadlineAt))
                .collect(Collectors.toList());

        if (deadlines.isEmpty()) {
            return """
            ✅ *ДЕДЛАЙНЫ НА 2 НЕДЕЛИ*
            
            🎉 *Отличные новости!*
            В ближайшие 2 недели нет дедлайнов! 🚀
            
            Можно сосредоточиться на изучении материала и подготовке к будущим занятиям.
            """;
        }

        StringBuilder deadlinesText = new StringBuilder();
        int urgentCount = 0;      // < 3 дней (красные)
        int normalCount = 0;      // 3-7 дней (желтые)
        int futureCount = 0;      // 7-14 дней (зеленые)

        for (int i = 0; i < deadlines.size(); i++) {
            Deadline d = deadlines.get(i);
            LocalDateTime deadlineTime = d.getDeadlineAt();

            // Рассчитываем разницу в днях
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(),
                    deadlineTime.toLocalDate()
            );

            String emoji;
            String daysText;
            String priorityLabel = "";

            // Срочные (менее 3 дней) - 🔴
            if (daysBetween < 3) {
                emoji = "🔴";
                urgentCount++;
                priorityLabel = " (СРОЧНО)";

                if (daysBetween == 0) {
                    // Рассчитываем оставшиеся часы для сегодняшнего дедлайна
                    long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, deadlineTime);
                    if (hoursLeft <= 12) {
                        daysText = String.format("⏰ Осталось %d ч.", hoursLeft);
                    } else {
                        daysText = "⏰ Сдать сегодня";
                    }
                } else if (daysBetween == 1) {
                    daysText = "⏳ Остался 1 день";
                } else {
                    daysText = String.format("⏳ Осталось %d д.", daysBetween);
                }
            }
            // Нормальные (3-7 дней) - 🟡
            else if (daysBetween <= 7) {
                emoji = "🟡";
                normalCount++;
                priorityLabel = " (НОРМАЛЬНЫЙ)";

                if (daysBetween == 3) {
                    daysText = "⏳ Осталось 3 дня";
                } else {
                    daysText = String.format("⏳ Осталось %d д.", daysBetween);
                }
            }
            // Будущие (7-14 дней) - 🟢
            else {
                emoji = "🟢";
                futureCount++;
                priorityLabel = " (БУДУЩИЙ)";

                if (daysBetween == 7) {
                    daysText = "📅 Через неделю";
                } else if (daysBetween == 14) {
                    daysText = "📅 Через 2 недели";
                } else {
                    daysText = String.format("📅 Через %d д.", daysBetween);
                }
            }

            // Добавляем номер дедлайна
            int deadlineNumber = i + 1;

            deadlinesText.append(String.format("%d. %s *%s*%s\n",
                            deadlineNumber, emoji, d.getTitle(), priorityLabel))
                    .append(String.format("   📅 %s\n",
                            deadlineTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
                    .append(String.format("   📝 %s\n",
                            d.getDescription() != null && !d.getDescription().isBlank() ?
                                    d.getDescription() : "Описание отсутствует"));

            // Добавляем ссылку, если она есть
            if (d.getLinkUrl() != null && !d.getLinkUrl().isBlank()) {
                String linkText = d.getLinkText() != null && !d.getLinkText().isBlank()
                        ? d.getLinkText()
                        : "Ссылка на задание";
                deadlinesText.append(String.format("   🔗 [%s](%s)\n", linkText, d.getLinkUrl()));
            }

            deadlinesText.append(String.format("   %s\n", daysText));

            // Добавляем разделитель между дедлайнами, но не после последнего
            if (i < deadlines.size() - 1) {
                deadlinesText.append("\n──────────\n\n");
            }
        }

        // Формируем общую статистику
        String statistics;
        if (urgentCount > 0) {
            statistics = String.format("""
            📈 *Статистика:*
            🔴 Срочных (< 3 дней): %d
            🟡 Нормальных (3-7 дней): %d
            🟢 Будущих (7-14 дней): %d
            📊 Всего: %d
            """,
                    urgentCount,
                    normalCount,
                    futureCount,
                    deadlines.size());
        } else if (normalCount > 0) {
            statistics = String.format("""
            📈 *Статистика:*
            🟡 Нормальных (3-7 дней): %d
            🟢 Будущих (7-14 дней): %d
            📊 Всего: %d
            """,
                    normalCount,
                    futureCount,
                    deadlines.size());
        } else {
            statistics = String.format("""
            📈 *Статистика:*
            🟢 Будущих (7-14 дней): %d
            📊 Всего: %d
            """,
                    futureCount,
                    deadlines.size());
        }

        // Формируем совет в зависимости от приоритета дедлайнов
        String advice;
        if (urgentCount > 0) {
            advice = """
            ⚠️ *Внимание!* Есть срочные дедлайны!
            💡 *Советы:*
            • Начните работу немедленно
            • Разделите задание на части
            • Сфокусируйтесь на самых срочных задачах
            """;
        } else if (normalCount > 0) {
            advice = """
            ⚠️ *Есть дедлайны на этой неделе*
            💡 *Советы:*
            • Составьте план на неделю
            • Распределите время равномерно
            • Начинайте выполнять задания заранее
            """;
        } else {
            advice = """
            ✅ *Все дедлайны в будущем*
            💡 *Советы:*
            • Можно спланировать работу заранее
            • Используйте время для углубленного изучения
            • Не откладывайте на последний момент
            """;
        }

        return String.format("""
        ⏰ *ДЕДЛАЙНЫ НА 2 НЕДЕЛИ*
        📅 *Период:* %s - %s
        
        %s
        
        %s
        
        %s
        
        🚀 *У вас всё получится!*
        """,
                now.format(DateTimeFormatter.ofPattern("dd.MM")),
                twoWeeksLater.format(DateTimeFormatter.ofPattern("dd.MM")),
                deadlinesText.toString(),
                statistics,
                advice);
    }

    /**
     * Отправляет напоминания перед парой для конкретного чата
     */
    private void sendBeforeClassRemindersForChat(BotChat chat, LocalDateTime now, int reminderMinutes) {
        LocalDate today = LocalDate.now();
        String currentWeekType = weekTypeService.getCurrentWeekType();
        int dayNumber = today.getDayOfWeek().getValue();

        // Получаем расписание на сегодня
        List<Schedule> scheduleList = scheduleRepository.findByDayOfWeek(dayNumber).stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .collect(Collectors.toList());

        for (Schedule schedule : scheduleList) {
            LocalTime classStart = schedule.getTimeStart();
            LocalTime reminderTime = classStart.minusMinutes(reminderMinutes);
            LocalTime currentTime = now.toLocalTime();

            // Проверяем, наступило ли время напоминания (с точностью до минуты)
            if (currentTime.getHour() == reminderTime.getHour() &&
                    currentTime.getMinute() == reminderTime.getMinute()) {

                String reminderMessage = generateBeforeClassReminder(schedule, reminderMinutes);
                try {
                    sendMessageToChat(chat.getChatId(), reminderMessage);
                    log.info("⏰ Напоминание отправлено в чат {} за {} минут до пары: {}",
                            chat.getChatId(), reminderMinutes, schedule.getSubject());
                } catch (Exception e) {
                    log.error("❌ Ошибка отправки напоминания в чат {}: {}", chat.getChatId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Генерирует напоминание перед парой
     */
    private String generateBeforeClassReminder(Schedule schedule, int minutesBefore) {
        String timeRange = String.format("%s-%s",
                schedule.getTimeStart().format(TIME_FORMATTER),
                schedule.getTimeEnd().format(TIME_FORMATTER));

        String location = (schedule.getIsOnline() != null && schedule.getIsOnline()) ?
                "💻 Онлайн" : "🏫 " + (schedule.getLocation() != null ? schedule.getLocation() : "Аудитория не указана");

        return String.format("""
            ⏰ *НАПОМИНАНИЕ О ПАРЕ*
            
            Через *%d минут* начинается пара:
            
            📖 *%s*
            ⏰ *%s*
            👨‍🏫 %s
            %s
            
            🚀 *Успевайте подготовиться!*
            """,
                minutesBefore,
                schedule.getSubject(),
                timeRange,
                schedule.getTeacher() != null ? schedule.getTeacher() : "Преподаватель не указан",
                location);
    }

    /**
     * Отправляет сообщение в чат
     */
    private void sendMessageToChat(Long chatId, String text) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .build();

        telegramBot.execute(message);
    }

    /**
     * Получает количество минут для напоминания перед парой из настроек чата
     */
    private int getReminderBeforeClassMinutes(BotChat chat) {
        Map<String, Object> settings = chat.getSettings();
        if (settings != null && settings.containsKey("reminder_before_class")) {
            Object value = settings.get("reminder_before_class");
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    log.warn("Некорректное значение reminder_before_class в чате {}: {}", chat.getChatId(), value);
                }
            }
        }

        // ВОЗВРАЩАЕМ ИЗ YAML КОНФИГУРАЦИИ
        return settingsConfig.getReminders().getBeforeClass().getMinutes();
    }

    /**
     * Тестовый метод: отправляет расписание в указанный чат
     */
    public void sendTestScheduleToChat(Long chatId) {
        try {
            String scheduleMessage = generateTodayScheduleMessage();
            sendMessageToChat(chatId, scheduleMessage);
            log.info("✅ Тестовое расписание отправлено в чат {}", chatId);
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки в чат {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Тестовый метод: отправляет дедлайны в указанный чат
     */
    public void sendTestDeadlinesToChat(Long chatId) {
        try {
            String deadlinesMessage = generateWeeklyDeadlinesMessage();
            sendMessageToChat(chatId, deadlinesMessage);
            log.info("✅ Тестовые дедлайны отправлены в чат {}", chatId);
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки в чат {}: {}", chatId, e.getMessage());
        }
    }

    private String getWeekTypeEmoji(String weekType) {
        if ("odd".equals(weekType)) {
            return "1️⃣";
        } else if ("even".equals(weekType)) {
            return "2️⃣";
        } else {
            return "🔄";
        }
    }
}