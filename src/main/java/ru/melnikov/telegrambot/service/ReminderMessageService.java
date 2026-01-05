package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.melnikov.telegrambot.bot.TelegramBot;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.BotChatRepository;
import ru.melnikov.telegrambot.repository.DeadlineRepository;
import ru.melnikov.telegrambot.repository.ScheduleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
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
    private final BotChatService botChatService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER_LONG = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // ====== ЕДИНЫЙ МЕТОД ГЕНЕРАЦИИ НАПОМИНАНИЯ О ПАРЕ ======

    /**
     * Единый метод для генерации красивого напоминания о паре
     */
    public String generateClassReminderMessage(Schedule schedule, int minutesBefore) {
        try {
            LocalDate today = LocalDate.now();

            // Получаем информацию о дне недели
            String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            // Получаем текущий тип недели
            String currentWeekType = weekTypeService.getCurrentWeekType();
            String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);
            String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);

            // Получаем информацию о типе недели для этой пары
            String scheduleWeekType = schedule.getWeekType() != null ? schedule.getWeekType() : "all";
            String pairWeekTypeEmoji = getWeekTypeEmoji(scheduleWeekType);
            String pairWeekTypeText = getWeekTypeText(scheduleWeekType);

            // Форматируем время пары
            String timeRange = String.format("%s-%s",
                    schedule.getTimeStart().format(TIME_FORMATTER),
                    schedule.getTimeEnd().format(TIME_FORMATTER));

            // Форматируем время до начала
            String timeLeft = formatTimeLeft(minutesBefore);

            // Получаем информацию о преподавателе
            String teacherInfo = formatTeacherInfo(schedule.getTeacher());

            // Получаем информацию о местоположении
            String locationInfo = formatLocationInfo(schedule);

            // Генерируем эмодзи в зависимости от оставшегося времени
            String timeEmoji = getTimeEmoji(minutesBefore);

            // Определяем уровень срочности
            String urgencyLevel = getUrgencyLevel(minutesBefore);

            // Генерируем подсказку в зависимости от времени
            String tip = getClassReminderTip(minutesBefore);

            // Формируем сообщение
            return String.format("""
                %s *НАПОМИНАНИЕ О ПРЕДСТОЯЩЕЙ ПАРЕ* %s
                
                📅 *%s* | %s %s
                
                ⏰ *До начала осталось:* %s
                %s
                
                %s *%s* (%s)
                👨‍🏫 *Преподаватель:* %s
                %s
                🕐 *Время:* %s
                
                %s
                
                🚀 *Удачной пары!*
                """,
                    timeEmoji, urgencyLevel,
                    dayName, weekTypeEmoji, weekTypeDisplay,
                    timeLeft,
                    tip,
                    pairWeekTypeEmoji, schedule.getSubject(), pairWeekTypeText,
                    teacherInfo,
                    locationInfo,
                    timeRange,
                    getClassPreparationTips(schedule.getSubject()));

        } catch (Exception e) {
            log.error("❌ Ошибка генерации напоминания о паре: {}", e.getMessage(), e);
            return String.format("""
                ⏰ *НАПОМИНАНИЕ О ПАРЕ*
                
                Через %d минут начинается пара:
                📖 *%s*
                🕐 %s-%s
                """,
                    minutesBefore,
                    schedule.getSubject(),
                    schedule.getTimeStart().format(TIME_FORMATTER),
                    schedule.getTimeEnd().format(TIME_FORMATTER));
        }
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ ======

    /**
     * Форматирует оставшееся время
     */
    private String formatTimeLeft(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return String.format("%d час%s", hours, hours > 1 ? "а" : "");
            } else {
                return String.format("%d час%s %d мин.", hours, hours > 1 ? "а" : "", remainingMinutes);
            }
        } else if (minutes >= 30) {
            return String.format("%d минут", minutes);
        } else if (minutes >= 15) {
            return String.format("%d минут (еще есть время!)", minutes);
        } else if (minutes >= 5) {
            return String.format("%d минут (пора собираться!)", minutes);
        } else {
            return String.format("%d минут (срочно!)", minutes);
        }
    }

    /**
     * Форматирует информацию о преподавателе
     */
    private String formatTeacherInfo(String teacher) {
        if (teacher == null || teacher.isBlank()) {
            return "Преподаватель не указан";
        }
        return teacher;
    }

    /**
     * Форматирует информацию о местоположении
     */
    private String formatLocationInfo(Schedule schedule) {
        if (schedule.getIsOnline() != null && schedule.getIsOnline()) {
            return "💻 *Онлайн*";
        } else if (schedule.getLocation() != null && !schedule.getLocation().isBlank()) {
            return String.format("📍 *%s*", schedule.getLocation());
        } else {
            return "📍 Место не указано";
        }
    }

    /**
     * Возвращает текст типа недели
     */
    private String getWeekTypeText(String weekType) {
        return switch (weekType) {
            case "odd" -> "числитель";
            case "even" -> "знаменатель";
            default -> "обе недели";
        };
    }

    /**
     * Возвращает эмодзи в зависимости от оставшегося времени
     */
    private String getTimeEmoji(int minutes) {
        if (minutes >= 60) return "🕐";
        if (minutes >= 30) return "⏰";
        if (minutes >= 15) return "⚠️";
        if (minutes >= 5) return "🔔";
        return "🚨";
    }

    /**
     * Возвращает уровень срочности
     */
    private String getUrgencyLevel(int minutes) {
        if (minutes >= 60) return "(ЗАРАНЕЕ)";
        if (minutes >= 30) return "(ВОВРЕМЯ)";
        if (minutes >= 15) return "(СОВСЕМ СКОРО)";
        if (minutes >= 5) return "(СРОЧНО)";
        return "(ОЧЕНЬ СРОЧНО!)";
    }

    /**
     * Возвращает подсказку в зависимости от времени
     */
    private String getClassReminderTip(int minutes) {
        if (minutes >= 60) {
            return "✨ *Можно спокойно подготовиться:*\n• Проверьте материалы\n• Соберите все необходимое";
        } else if (minutes >= 30) {
            return "📚 *Пора готовиться:*\n• Возьмите конспекты\n• Проверьте подключение";
        } else if (minutes >= 15) {
            return "⚡ *Время поджимает:*\n• Быстрая подготовка\n• Зарядите устройства";
        } else if (minutes >= 5) {
            return "🚨 *Срочно!*:\n• Берите самое необходимое\n• Выходите заранее";
        } else {
            return "🔥 *Опаздываете!*:\n• Бегите!";
        }
    }

    /**
     * Возвращает советы по подготовке к конкретному предмету
     */
    private String getClassPreparationTips(String subject) {
        if (subject == null) return "";

        String lowerSubject = subject.toLowerCase();

        if (lowerSubject.contains("матем") || lowerSubject.contains("алг")) {
            return "📐 *Совет:* Возьмите калькулятор и тетрадь с формулами";
        } else if (lowerSubject.contains("физик")) {
            return "⚛️ *Совет:* Повторите основные законы и формулы";
        } else if (lowerSubject.contains("хими")) {
            return "🧪 *Совет:* Вспомните таблицу Менделеева";
        } else if (lowerSubject.contains("информат") || lowerSubject.contains("програм")) {
            return "💻 *Совет:* Зарядите ноутбук и проверьте код";
        } else if (lowerSubject.contains("англ") || lowerSubject.contains("язык")) {
            return "🇬🇧 *Совет:* Повторите словарный запас";
        } else if (lowerSubject.contains("истор")) {
            return "📜 *Совет:* Вспомните ключевые даты";
        } else if (lowerSubject.contains("биолог")) {
            return "🧬 *Совет:* Повторите термины и схемы";
        } else {
            return "📝 *Совет:* Возьмите конспекты и ручку";
        }
    }

    // ====== ОСНОВНЫЕ МЕТОДЫ ОТПРАВКИ ======

    /**
     * Отправляет расписание на сегодня во все активные чаты
     */
    public void sendDailyScheduleToAllChats() {
        log.info("📅 Запуск отправки ежедневного расписания...");

        List<BotChat> activeChats = botChatService.findAllActiveChats();

        for (BotChat chat : activeChats) {
            try {
                if (shouldSendScheduleToChat(chat)) {
                    sendScheduleToChat(chat.getChatId());
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("Ошибка отправки расписания в чат {}: {}", chat.getChatId(), e.getMessage());
            }
        }

        log.info("✅ Ежедневное расписание отправлено в {} чатов", activeChats.size());
    }

    /**
     * Отправляет дедлайны во все активные чаты
     */
    public void sendWeeklyDeadlinesToAllChats() {
        log.info("⏰ Запуск отправки недельных дедлайнов...");

        List<BotChat> activeChats = botChatService.findAllActiveChats();

        for (BotChat chat : activeChats) {
            try {
                if (shouldSendDeadlinesToChat(chat)) {
                    sendDeadlinesToChat(chat.getChatId());
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("Ошибка отправки дедлайнов в чат {}: {}", chat.getChatId(), e.getMessage());
            }
        }

        log.info("✅ Недельные дедлайны отправлены в {} чатов", activeChats.size());
    }

    /**
     * Отправляет напоминание о начале пары (ЕДИНЫЙ МЕТОД)
     */
    public void sendClassReminder(Long chatId, Schedule schedule, int minutesBefore) {
        try {
            Optional<Integer> botTopicId = botChatService.getBotTopicId(chatId);

            // Используем единый метод генерации
            String reminderText = generateClassReminderMessage(schedule, minutesBefore);

            sendMessageToChat(chatId, botTopicId.orElse(null), reminderText, false);

            log.info("✅ Напоминание отправлено в чат {}: '{}' за {} минут",
                    chatId, schedule.getSubject(), minutesBefore);

        } catch (Exception e) {
            log.error("❌ Ошибка отправки напоминания в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет сообщение в чат с учетом темы бота если она установлена
     */
    public void sendMessageToChat(Long chatId, String text) {
        sendMessageToChat(chatId, null, text, false);
    }

    /**
     * Отправляет сообщение в чат с возможностью указать тему
     */
    public void sendMessageToChat(Long chatId, Integer messageThreadId, String text, boolean removeKeyboard) {
        try {
            Optional<Integer> botTopicId = botChatService.getBotTopicId(chatId);
            Integer targetTopicId = messageThreadId != null ? messageThreadId : botTopicId.orElse(null);

            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown");

            if (targetTopicId != null) {
                builder.messageThreadId(targetTopicId);
                log.debug("Отправка уведомления в тему ID: {} для чата {}", targetTopicId, chatId);
            }

            if (removeKeyboard) {
                builder.replyMarkup(new ReplyKeyboardRemove(true));
            }

            telegramBot.execute(builder.build());

            log.info("✅ Уведомление отправлено в чат {} (тема: {})",
                    chatId, targetTopicId != null ? targetTopicId : "без темы");

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения в чат {}: {}", chatId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка при отправке в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет расписание в конкретный чат
     */
    public void sendScheduleToChat(Long chatId) {
        try {
            Optional<Integer> botTopicId = botChatService.getBotTopicId(chatId);
            String scheduleText = formatDailySchedule();
            sendMessageToChat(chatId, botTopicId.orElse(null), scheduleText, false);
        } catch (Exception e) {
            log.error("Ошибка отправки расписания в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет дедлайны в конкретный чат
     */
    public void sendDeadlinesToChat(Long chatId) {
        try {
            Optional<Integer> botTopicId = botChatService.getBotTopicId(chatId);
            String deadlinesText = formatDeadlines();
            sendMessageToChat(chatId, botTopicId.orElse(null), deadlinesText, false);
        } catch (Exception e) {
            log.error("Ошибка отправки дедлайнов в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет тестовое сообщение для проверки темы
     */
    public void sendTestMessageToChat(Long chatId, String messageType) {
        try {
            Optional<Integer> botTopicId = botChatService.getBotTopicId(chatId);

            String testText = String.format("""
                🔔 *ТЕСТОВОЕ УВЕДОМЛЕНИЕ: %s*
                
                📅 *Дата:* %s
                ⏰ *Время:* %s
                📌 *Тема ID:* %s
                
                ✅ *Это тестовое сообщение для проверки работы системы уведомлений.*
                """,
                    messageType,
                    LocalDate.now().format(DATE_FORMATTER),
                    LocalTime.now().format(TIME_FORMATTER_LONG),
                    botTopicId.map(String::valueOf).orElse("не установлена"));

            sendMessageToChat(chatId, botTopicId.orElse(null), testText, false);

        } catch (Exception e) {
            log.error("Ошибка отправки тестового сообщения в чат {}: {}", chatId, e.getMessage(), e);
        }
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

    // ====== МЕТОДЫ ГЕНЕРАЦИИ ДЛЯ УПРАВЛЕНИЯ ======

    /**
     * Генерирует сообщение с расписанием на сегодня
     */
    public String generateTodayScheduleMessage() {
        LocalDate today = LocalDate.now();
        String currentWeekType = weekTypeService.getCurrentWeekType();
        String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);
        String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);

        int dayNumber = today.getDayOfWeek().getValue();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

        List<Schedule> allScheduleList = scheduleRepository.findByDayOfWeek(dayNumber);
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
    public String generateWeeklyDeadlinesMessage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoWeeksLater = now.plusDays(14);

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
        int urgentCount = 0;
        int normalCount = 0;
        int futureCount = 0;

        for (int i = 0; i < deadlines.size(); i++) {
            Deadline d = deadlines.get(i);
            LocalDateTime deadlineTime = d.getDeadlineAt();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(),
                    deadlineTime.toLocalDate()
            );

            String emoji;
            String daysText;
            String priorityLabel = "";

            if (daysBetween < 3) {
                emoji = "🔴";
                urgentCount++;
                priorityLabel = " (СРОЧНО)";
                if (daysBetween == 0) {
                    long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, deadlineTime);
                    daysText = hoursLeft <= 12 ? String.format("⏰ Осталось %d ч.", hoursLeft) : "⏰ Сдать сегодня";
                } else if (daysBetween == 1) {
                    daysText = "⏳ Остался 1 день";
                } else {
                    daysText = String.format("⏳ Осталось %d д.", daysBetween);
                }
            } else if (daysBetween <= 7) {
                emoji = "🟡";
                normalCount++;
                priorityLabel = " (НОРМАЛЬНЫЙ)";
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            } else {
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

            int deadlineNumber = i + 1;
            deadlinesText.append(String.format("%d. %s *%s*%s\n",
                            deadlineNumber, emoji, d.getTitle(), priorityLabel))
                    .append(String.format("   📅 %s\n",
                            deadlineTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
                    .append(String.format("   📝 %s\n",
                            d.getDescription() != null && !d.getDescription().isBlank() ?
                                    d.getDescription() : "Описание отсутствует"));

            if (d.getLinkUrl() != null && !d.getLinkUrl().isBlank()) {
                String linkText = d.getLinkText() != null && !d.getLinkText().isBlank()
                        ? d.getLinkText()
                        : "Ссылка на задание";
                deadlinesText.append(String.format("   🔗 [%s](%s)\n", linkText, d.getLinkUrl()));
            }

            deadlinesText.append(String.format("   %s\n", daysText));

            if (i < deadlines.size() - 1) {
                deadlinesText.append("\n──────────\n\n");
            }
        }

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

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======

    private String getWeekTypeEmoji(String weekType) {
        if ("odd".equals(weekType)) {
            return "1️⃣";
        } else if ("even".equals(weekType)) {
            return "2️⃣";
        } else {
            return "🔄";
        }
    }

    /**
     * Проверяет, нужно ли отправлять расписание в чат
     */
    private boolean shouldSendScheduleToChat(BotChat chat) {
        if (chat == null || !Boolean.TRUE.equals(chat.getIsActive())) {
            return false;
        }

        Map<String, Object> settings = chat.getSettings();
        if (settings == null) {
            return true;
        }

        return (boolean) settings.getOrDefault("schedule_notifications", true);
    }

    /**
     * Проверяет, нужно ли отправлять дедлайны в чат
     */
    private boolean shouldSendDeadlinesToChat(BotChat chat) {
        if (chat == null || !Boolean.TRUE.equals(chat.getIsActive())) {
            return false;
        }

        Map<String, Object> settings = chat.getSettings();
        if (settings == null) {
            return true;
        }

        return (boolean) settings.getOrDefault("deadline_notifications", true);
    }

    /**
     * Форматирует расписание на сегодня
     */
    private String formatDailySchedule() {
        LocalDate today = LocalDate.now();
        int dayNumber = today.getDayOfWeek().getValue();
        String currentWeekType = weekTypeService.getCurrentWeekType();
        String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);
        String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);

        List<Schedule> allScheduleList = scheduleService.findEntitiesByDay(dayNumber);
        List<Schedule> filteredScheduleList = allScheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .toList();

        if (filteredScheduleList.isEmpty()) {
            return String.format("""
                📭 *Сегодня занятий нет!* 📭
                📅 *День:* %s
                🗓️ *Тип недели:* %s %s
                
                🎉 *Можно отдохнуть или заняться саморазвитием*
                """,
                    today.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE),
                    weekTypeEmoji, weekTypeDisplay);
        }

        StringBuilder scheduleText = new StringBuilder();
        scheduleText.append(String.format("📋 *РАСПИСАНИЕ НА СЕГОДНЯ*\n"))
                .append(String.format("📅 *День:* %s\n",
                        today.getDayOfWeek().getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE)))
                .append(String.format("🗓️ *Тип недели:* %s %s\n\n",
                        weekTypeEmoji, weekTypeDisplay));

        for (int i = 0; i < filteredScheduleList.size(); i++) {
            Schedule s = filteredScheduleList.get(i);
            String timeRange = String.format("%s-%s",
                    s.getTimeStart().format(TIME_FORMATTER),
                    s.getTimeEnd().format(TIME_FORMATTER));

            String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
            String pairWeekTypeEmoji = "odd".equals(scheduleWeekType) ? "1️⃣" :
                    "even".equals(scheduleWeekType) ? "2️⃣" : "🔄";
            String onlineEmoji = Boolean.TRUE.equals(s.getIsOnline()) ? "💻" : "🏫";

            scheduleText.append(String.format("%d. %s %s\n", i + 1, pairWeekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s\n", s.getSubject()));

            if (s.getTeacher() != null && !s.getTeacher().isBlank()) {
                scheduleText.append(String.format("   👨‍🏫 %s\n", s.getTeacher()));
            }

            if (s.getLocation() != null && !s.getLocation().isBlank()) {
                String location = Boolean.TRUE.equals(s.getIsOnline()) ? "Онлайн" : s.getLocation();
                scheduleText.append(String.format("   📍 %s\n", location));
            }

            scheduleText.append("\n");
        }

        return scheduleText.toString();
    }

    /**
     * Форматирует дедлайны
     */
    private String formatDeadlines() {
        List<Deadline> allDeadlines = deadlineService.findAllDeadlinesSorted();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        List<Deadline> filteredDeadlines = allDeadlines.stream()
                .filter(d -> {
                    LocalDateTime deadlineTime = d.getDeadlineAt();
                    return deadlineTime.isAfter(now) ||
                            (deadlineTime.isBefore(now) && deadlineTime.isAfter(sevenDaysAgo));
                })
                .sorted((d1, d2) -> d1.getDeadlineAt().compareTo(d2.getDeadlineAt()))
                .toList();

        if (filteredDeadlines.isEmpty()) {
            return """
                ✅ *Все дедлайны выполнены!* ✅
                
                🎉 *Отличная работа!* 🎉
                Все задания сданы вовремя.
                """;
        }

        StringBuilder deadlinesText = new StringBuilder();
        deadlinesText.append("⏰ *АКТУАЛЬНЫЕ ДЕДЛАЙНЫ*\n\n");

        for (int i = 0; i < filteredDeadlines.size(); i++) {
            Deadline deadline = filteredDeadlines.get(i);
            LocalDateTime deadlineTime = deadline.getDeadlineAt();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(),
                    deadlineTime.toLocalDate()
            );

            String emoji;
            String daysText;

            if (deadlineTime.isBefore(now)) {
                emoji = "🔴";
                long daysOverdue = Math.abs(daysBetween);
                daysText = daysOverdue == 0 ? "⚠️ Просрочено сегодня" :
                        String.format("⚠️ Просрочено на %d д.", daysOverdue);
            } else if (daysBetween == 0) {
                emoji = "🔴";
                daysText = "⏰ Сдать сегодня";
            } else if (daysBetween <= 2) {
                emoji = "🔴";
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            } else if (daysBetween <= 7) {
                emoji = "🟡";
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            } else {
                emoji = "🟢";
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            }

            deadlinesText.append(String.format("%s *%s*\n", emoji, deadline.getTitle()))
                    .append(String.format("   📅 %s\n", deadlineTime.format(DATETIME_FORMATTER)))
                    .append(String.format("   📝 %s\n",
                            deadline.getDescription() != null && !deadline.getDescription().isBlank() ?
                                    deadline.getDescription() : "Описание отсутствует"));

            if (deadline.getLinkUrl() != null && !deadline.getLinkUrl().isBlank()) {
                String linkText = deadline.getLinkText() != null && !deadline.getLinkText().isBlank() ?
                        deadline.getLinkText() : "Ссылка на задание";
                deadlinesText.append(String.format("   🔗 [%s](%s)\n", linkText, deadline.getLinkUrl()));
            }

            deadlinesText.append(String.format("   %s\n\n", daysText));
        }

        return deadlinesText.toString();
    }
}