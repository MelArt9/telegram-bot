package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.melnikov.telegrambot.bot.TelegramBot;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.BotChatRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartReminderService {

    private final BotSettingsConfig settingsConfig;
    private final BotChatRepository botChatRepository;
    private final ScheduleRepository scheduleRepository;
    private final ReminderMessageService reminderMessageService;
    private final TelegramBot telegramBot;
    private final WeekTypeService weekTypeService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    /**
     * Ежедневная проверка и отправка расписания
     * Проверяем каждую минуту в течение часа
     */
    @Scheduled(cron = "0 * * * * *") // Каждую минуту
    public void checkAndSendScheduleReminders() {
        // Получаем настройки из YAML
        boolean scheduleEnabled = settingsConfig.getReminders().getSchedule().getEnabled();
        LocalTime scheduleTime = settingsConfig.getReminders().getSchedule().getTimeAsLocalTime();
        String scheduleDays = settingsConfig.getReminders().getSchedule().getDays();
        String scheduleDaysDescription = settingsConfig.getReminders().getSchedule().getDaysDescription();

        // Проверяем, включены ли напоминания о расписании
        if (!scheduleEnabled) {
            log.debug("⏸️ Напоминания о расписании отключены в конфигурации YAML");
            return;
        }

        try {
            LocalTime currentTime = LocalTime.now();

            // Проверяем, наступило ли время отправки (с точностью до минуты)
            if (currentTime.getHour() == scheduleTime.getHour() &&
                    currentTime.getMinute() == scheduleTime.getMinute()) {

                // Проверяем, должен ли сегодня отправляться reminder по дням недели
                if (shouldSendToday(scheduleDays)) {
                    log.info("⏰ Время отправки расписания! {} (сейчас {})",
                            scheduleTime.format(TIME_FORMATTER),
                            currentTime.format(TIME_FORMATTER));
                    log.info("📅 Дни недели для расписания: {}", scheduleDaysDescription);

                    reminderMessageService.sendDailyScheduleToAllChats();
                    log.info("✅ Расписание успешно отправлено в {}:{}",
                            currentTime.getHour(), currentTime.getMinute());
                } else {
                    log.debug("⏸️ Сегодня не день для отправки расписания (дни недели: {})",
                            scheduleDaysDescription);
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке расписания: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверка напоминаний перед парой каждую минуту
     */
    @Scheduled(cron = "0 * * * * *") // Каждую минуту
    public void checkBeforeClassReminders() {
        // Получаем минуты из YML
        int minutesBefore = settingsConfig.getReminders().getBeforeClass().getMinutes();
        boolean enabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

        if (!enabled) {
            log.debug("⏸️ Напоминания перед парой отключены в конфигурации YAML");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime checkTime = now.toLocalTime();

        log.debug("⏳ Проверка напоминаний перед парой (за {} минут) в {}",
                minutesBefore, checkTime.format(TIME_FORMATTER));

        // Получаем все активные группы с включенными напоминаниями
        List<Object[]> activeChats = botChatRepository.findAllActiveGroupsWithBeforeClass();

        if (activeChats.isEmpty()) {
            log.debug("📭 Нет активных чатов с включенными напоминаниями перед парой");
            return;
        }

        log.debug("🔍 Найдено {} активных чатов для проверки", activeChats.size());

        for (Object[] chatData : activeChats) {
            Long chatId = (Long) chatData[0];
            boolean beforeClassEnabled = (boolean) chatData[1];

            if (beforeClassEnabled) {
                // Логика проверки и отправки напоминаний
                sendBeforeClassReminderIfNeeded(chatId, minutesBefore, checkTime, now.toLocalDate());
            }
        }
    }

    /**
     * Отправка напоминания перед парой, если нужно
     */
    private void sendBeforeClassReminderIfNeeded(Long chatId, int minutesBefore, LocalTime checkTime, LocalDate today) {
        try {
            // Определяем день недели (1-7)
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            int dayNumber = dayOfWeek.getValue();

            // Получаем текущий тип недели
            String currentWeekType = weekTypeService.getCurrentWeekType();

            // Получаем все пары на сегодня
            List<Schedule> allSchedules = scheduleRepository.findByDayOfWeek(dayNumber);

            if (allSchedules.isEmpty()) {
                log.debug("📭 Нет пар в базе данных для {} (день {})",
                        dayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE), dayNumber);
                return;
            }

            // Фильтруем пары для текущего типа недели
            List<Schedule> todaySchedules = allSchedules.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                        return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                    })
                    .filter(s -> s.getTimeStart() != null)
                    .sorted((s1, s2) -> s1.getTimeStart().compareTo(s2.getTimeStart()))
                    .toList();

            if (todaySchedules.isEmpty()) {
                log.debug("📭 Нет пар на сегодня для типа недели: {}", currentWeekType);
                return;
            }

            log.debug("📅 Найдено {} пар на сегодня для чата {} (тип недели: {})",
                    todaySchedules.size(), chatId, currentWeekType);

            // Проверяем каждую пару
            for (Schedule schedule : todaySchedules) {
                LocalTime classStartTime = schedule.getTimeStart();

                // Вычисляем время напоминания
                LocalTime reminderTime = classStartTime.minusMinutes(minutesBefore);

                // Проверяем, пора ли отправлять напоминание
                if (isTimeToSendReminder(checkTime, reminderTime)) {
                    // Отправляем напоминание
                    sendBeforeClassReminder(chatId, schedule, minutesBefore, currentWeekType);
                    log.info("✅ Отправлено напоминание для чата {} о паре '{}' (начало в {}, напоминание за {} минут)",
                            chatId, schedule.getSubject(),
                            classStartTime.format(TIME_FORMATTER), minutesBefore);
                }
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при проверке напоминаний для чата {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Проверяет, пора ли отправлять напоминание
     */
    private boolean isTimeToSendReminder(LocalTime currentTime, LocalTime reminderTime) {
        // Сравниваем время с точностью до минуты
        LocalTime currentTimeRounded = LocalTime.of(currentTime.getHour(), currentTime.getMinute());
        LocalTime reminderTimeRounded = LocalTime.of(reminderTime.getHour(), reminderTime.getMinute());

        boolean shouldSend = currentTimeRounded.equals(reminderTimeRounded);

        if (shouldSend) {
            log.debug("⏰ Время отправки напоминания! Текущее: {}, Напоминание: {}",
                    currentTimeRounded.format(TIME_FORMATTER),
                    reminderTimeRounded.format(TIME_FORMATTER));
        }

        return shouldSend;
    }

    /**
     * Отправляет напоминание о предстоящей паре
     */
    private void sendBeforeClassReminder(Long chatId, Schedule schedule, int minutesBefore, String weekType) {
        try {
            String dayName = DayOfWeek.of(schedule.getDayOfWeek())
                    .getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(weekType);
            String weekTypeName = weekTypeService.getWeekTypeDisplayName(weekType);

            String timeRange = String.format("%s-%s",
                    schedule.getTimeStart().format(TIME_FORMATTER),
                    schedule.getTimeEnd().format(TIME_FORMATTER));

            String location = schedule.getIsOnline() != null && schedule.getIsOnline() ?
                    "💻 Онлайн" :
                    (schedule.getLocation() != null ? "📍 " + schedule.getLocation() : "🏫 Аудитория не указана");

            String teacher = schedule.getTeacher() != null ? schedule.getTeacher() : "Преподаватель не указан";

            // Эмодзи для типа недели пары
            String scheduleWeekType = schedule.getWeekType() != null ? schedule.getWeekType() : "all";
            String pairWeekTypeEmoji = "odd".equals(scheduleWeekType) ? "1️⃣" :
                    "even".equals(scheduleWeekType) ? "2️⃣" : "🔄";
            String pairWeekTypeText = "odd".equals(scheduleWeekType) ? "числитель" :
                    "even".equals(scheduleWeekType) ? "знаменатель" : "обе недели";

            String message = String.format("""
                    🔔 *НАПОМИНАНИЕ О ПРЕДСТОЯЩЕЙ ПАРЕ*
                    
                    📅 *%s* | %s %s
                    ⏰ *До начала осталось:* %d минут
                    
                    %s *%s* (%s)
                    👨‍🏫 *Преподаватель:* %s
                    %s
                    🕐 *Время:* %s
                    
                    🚀 *Удачной пары!*
                    """,
                    dayName, weekTypeEmoji, weekTypeName,
                    minutesBefore,
                    pairWeekTypeEmoji, schedule.getSubject(), pairWeekTypeText,
                    teacher,
                    location,
                    timeRange);

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(message)
                    .parseMode(ParseMode.MARKDOWN)
                    .build();

            telegramBot.execute(sendMessage);

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки напоминания в чат {}: {}", chatId, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка при отправке напоминания в чат {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Ежедневная проверка и отправка дедлайнов
     */
    @Scheduled(cron = "0 * * * * *") // Каждую минуту
    public void checkAndSendDeadlineReminders() {
        // Получаем настройки из YAML
        boolean deadlinesEnabled = settingsConfig.getReminders().getDeadlines().getEnabled();
        LocalTime deadlineTime = settingsConfig.getReminders().getDeadlines().getTimeAsLocalTime();
        String deadlineDays = settingsConfig.getReminders().getDeadlines().getDays();
        String deadlineDaysDescription = settingsConfig.getReminders().getDeadlines().getDaysDescription();

        // Проверяем, включены ли напоминания о дедлайнах
        if (!deadlinesEnabled) {
            log.debug("⏸️ Напоминания о дедлайнах отключены в конфигурации YAML");
            return;
        }

        try {
            LocalTime currentTime = LocalTime.now();

            // Проверяем, наступило ли время отправки
            if (currentTime.getHour() == deadlineTime.getHour() &&
                    currentTime.getMinute() == deadlineTime.getMinute()) {

                // Проверяем дни недели
                if (shouldSendToday(deadlineDays)) {
                    log.info("⏰ Время отправки дедлайнов! {} (сейчас {})",
                            deadlineTime.format(TIME_FORMATTER),
                            currentTime.format(TIME_FORMATTER));
                    log.info("📅 Дни недели для дедлайнов: {}", deadlineDaysDescription);

                    reminderMessageService.sendWeeklyDeadlinesToAllChats();
                    log.info("✅ Дедлайны успешно отправлены в {}:{}",
                            currentTime.getHour(), currentTime.getMinute());
                } else {
                    log.debug("⏸️ Сегодня не день для отправки дедлайнов (дни недели: {})",
                            deadlineDaysDescription);
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке дедлайнов: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверяет, нужно ли отправлять reminder сегодня
     * @param daysPattern Паттерн дней недели из YAML (7 символов: 1-включен, 0-выключен)
     */
    private boolean shouldSendToday(String daysPattern) {
        if (daysPattern == null || daysPattern.length() != 7) {
            log.warn("❌ Некорректный паттерн дней недели из YAML: {}", daysPattern);
            return false;
        }

        // Получаем индекс дня недели (0-Пн, 1-Вт, ..., 6-Вс)
        int dayOfWeekIndex = LocalDate.now().getDayOfWeek().getValue() - 1;

        if (dayOfWeekIndex >= 0 && dayOfWeekIndex < daysPattern.length()) {
            char dayChar = daysPattern.charAt(dayOfWeekIndex);
            boolean shouldSend = dayChar == '1';

            log.debug("Проверка дня недели: индекс={}, символ={}, отправлять={}",
                    dayOfWeekIndex, dayChar, shouldSend);

            return shouldSend;
        }

        return false;
    }

    /**
     * Тестовый метод для ручной отправки расписания
     */
    public void sendTestScheduleNow() {
        try {
            log.info("🚀 Тестовая отправка расписания (игнорируя настройки времени)...");
            reminderMessageService.sendDailyScheduleToAllChats();
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки расписания: {}", e.getMessage(), e);
        }
    }

    /**
     * Тестовый метод для ручной отправки дедлайнов
     */
    public void sendTestDeadlinesNow() {
        try {
            log.info("🚀 Тестовая отправка дедлайнов (игнорируя настройки времени)...");
            reminderMessageService.sendWeeklyDeadlinesToAllChats();
        } catch (Exception e) {
            log.error("❌ Ошибка тестовой отправки дедлайнов: {}", e.getMessage(), e);
        }
    }

    /**
     * Возвращает текущие настройки из YAML для отладки
     */
    public String getCurrentSettingsInfo() {
        return String.format("""
            📊 ТЕКУЩИЕ НАСТРОЙКИ ИЗ YAML:
            
            📅 Расписание:
            • Включено: %s
            • Время: %s
            • Дни недели: %s (%s)
            
            ⏰ Дедлайны:
            • Включено: %s
            • Время: %s
            • Дни недели: %s (%s)
            
            ⏳ Перед парой:
            • Включено: %s
            • За минут: %d
            
            ⚙️ Общие:
            • Шедулер включен: %s
            • Интервал проверки: %s
            
            📅 Тип недели:
            • Дата отсчета: %s
            • Тип на дату отсчета: %s
            
            🕐 Текущее время: %s
            """,

                // Расписание
                settingsConfig.getReminders().getSchedule().getEnabled(),
                settingsConfig.getReminders().getSchedule().getTime(),
                settingsConfig.getReminders().getSchedule().getDays(),
                settingsConfig.getReminders().getSchedule().getDaysDescription(),

                // Дедлайны
                settingsConfig.getReminders().getDeadlines().getEnabled(),
                settingsConfig.getReminders().getDeadlines().getTime(),
                settingsConfig.getReminders().getDeadlines().getDays(),
                settingsConfig.getReminders().getDeadlines().getDaysDescription(),

                // Перед парой
                settingsConfig.getReminders().getBeforeClass().getEnabled(),
                settingsConfig.getReminders().getBeforeClass().getMinutes(),

                // Общие
                settingsConfig.getReminders().getScheduler().getEnabled(),
                settingsConfig.getReminders().getScheduler().getCheckInterval(),

                // Тип недели
                settingsConfig.getReminders().getWeekType().getReferenceDate(),
                settingsConfig.getReminders().getWeekType().getReferenceWeekType(),

                // Текущее время
                LocalTime.now().format(TIME_FORMATTER)
        );
    }

    /**
     * Возвращает, будет ли сегодня отправка по текущим настройкам
     */
    public Map<String, Object> getTodaySendStatus() {
        Map<String, Object> status = new HashMap<>();

        // Текущий день недели
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;
        String todayName = dayNames[todayIndex];

        // Получаем символы для сегодня
        String scheduleDays = settingsConfig.getReminders().getSchedule().getDays();
        String deadlineDays = settingsConfig.getReminders().getDeadlines().getDays();

        char scheduleChar = '0';
        char deadlineChar = '0';

        if (scheduleDays != null && scheduleDays.length() > todayIndex) {
            scheduleChar = scheduleDays.charAt(todayIndex);
        }

        if (deadlineDays != null && deadlineDays.length() > todayIndex) {
            deadlineChar = deadlineDays.charAt(todayIndex);
        }

        status.put("scheduleEnabled", settingsConfig.getReminders().getSchedule().getEnabled());
        status.put("deadlinesEnabled", settingsConfig.getReminders().getDeadlines().getEnabled());
        status.put("scheduleToday", scheduleChar == '1');
        status.put("deadlinesToday", deadlineChar == '1');
        status.put("today", todayName);
        status.put("scheduleTime", settingsConfig.getReminders().getSchedule().getTime());
        status.put("deadlineTime", settingsConfig.getReminders().getDeadlines().getTime());
        status.put("beforeClassEnabled", settingsConfig.getReminders().getBeforeClass().getEnabled());
        status.put("beforeClassMinutes", settingsConfig.getReminders().getBeforeClass().getMinutes());

        // Добавляем информацию о текущем времени
        status.put("currentTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        status.put("scheduleWillSend",
                settingsConfig.getReminders().getSchedule().getEnabled() &&
                        scheduleChar == '1');
        status.put("deadlinesWillSend",
                settingsConfig.getReminders().getDeadlines().getEnabled() &&
                        deadlineChar == '1');

        return status;
    }

    /**
     * Проверяет, активны ли сейчас напоминания перед парой
     * Для тестирования и отладки
     */
    public Map<String, Object> checkBeforeClassStatus() {
        Map<String, Object> status = new HashMap<>();

        int minutesBefore = settingsConfig.getReminders().getBeforeClass().getMinutes();
        boolean enabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

        status.put("enabled", enabled);
        status.put("minutes", minutesBefore);
        status.put("currentTime", LocalTime.now().format(TIME_FORMATTER));

        if (enabled) {
            // Получаем активные чаты
            List<Object[]> activeChats = botChatRepository.findAllActiveGroupsWithBeforeClass();
            status.put("activeChatsCount", activeChats.size());

            // Проверяем, есть ли пары на сегодня
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            int dayNumber = dayOfWeek.getValue();
            String currentWeekType = weekTypeService.getCurrentWeekType();

            List<Schedule> allSchedules = scheduleRepository.findByDayOfWeek(dayNumber);
            List<Schedule> todaySchedules = allSchedules.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                        return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                    })
                    .filter(s -> s.getTimeStart() != null)
                    .sorted((s1, s2) -> s1.getTimeStart().compareTo(s2.getTimeStart()))
                    .toList();

            status.put("todaySchedulesCount", todaySchedules.size());
            status.put("today", today.toString());
            status.put("dayOfWeek", dayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE));
            status.put("weekType", currentWeekType);

            // Проверяем ближайшие напоминания
            List<Map<String, Object>> upcomingReminders = todaySchedules.stream()
                    .map(schedule -> {
                        Map<String, Object> reminderInfo = new HashMap<>();
                        reminderInfo.put("subject", schedule.getSubject());
                        reminderInfo.put("startTime", schedule.getTimeStart().format(TIME_FORMATTER));
                        reminderInfo.put("reminderTime", schedule.getTimeStart().minusMinutes(minutesBefore).format(TIME_FORMATTER));
                        reminderInfo.put("minutesBefore", minutesBefore);
                        return reminderInfo;
                    })
                    .toList();

            status.put("upcomingReminders", upcomingReminders);
        }

        return status;
    }
}