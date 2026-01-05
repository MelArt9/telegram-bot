// Файл: /src/main/java/ru/melnikov/telegrambot/scheduler/ReminderScheduler.java
package ru.melnikov.telegrambot.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderMessageService reminderMessageService;
    private final BotChatService botChatService;
    private final ScheduleService scheduleService;
    private final BotSettingsConfig settingsConfig;
    private final WeekTypeService weekTypeService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Ежедневная отправка расписания
     */
    @Scheduled(cron = "0 0 8 * * *") // Каждый день в 8:00
    public void sendDailySchedule() {
        log.info("⏰ Запуск ежедневной отправки расписания...");
        try {
            reminderMessageService.sendDailyScheduleToAllChats();
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке ежедневного расписания: {}", e.getMessage(), e);
        }
    }

    /**
     * Еженедельная отправка дедлайнов
     */
    @Scheduled(cron = "0 0 9 * * MON") // Каждый понедельник в 9:00
    public void sendWeeklyDeadlines() {
        log.info("📋 Запуск еженедельной отправки дедлайнов...");
        try {
            reminderMessageService.sendWeeklyDeadlinesToAllChats();
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке недельных дедлайнов: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверка пар каждую минуту для отправки напоминаний перед началом
     */
    @Scheduled(fixedDelay = 60000) // Каждую минуту
    public void checkClassReminders() {
        try {
            int minutesBefore = settingsConfig.getReminders().getBeforeClass().getMinutes();
            boolean enabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

            if (!enabled) {
                log.debug("⏸️ Напоминания перед парой отключены в YML");
                return;
            }

            // Текущий день недели и тип недели
            LocalDate today = LocalDate.now();
            int currentDayOfWeek = today.getDayOfWeek().getValue();
            String currentWeekType = weekTypeService.getCurrentWeekType();

            // Время проверки (текущее время + минуты напоминания)
            LocalTime checkTime = LocalTime.now().plusMinutes(minutesBefore);

            // Получаем все активные группы
            List<BotChat> activeGroups = botChatService.getRepository().findAllActiveGroups();

            log.debug("🔍 Проверка напоминаний за {} минут для {} групп", minutesBefore, activeGroups.size());

            for (BotChat group : activeGroups) {
                try {
                    // Проверяем, включены ли напоминания перед парой для этой группы
                    Map<String, Object> settings = group.getSettings();
                    boolean beforeClassEnabled = settings != null &&
                            (boolean) settings.getOrDefault("before_class_enabled",
                                    settingsConfig.getReminders().getBeforeClass().getEnabled());

                    if (!beforeClassEnabled) {
                        log.debug("⏸️ Напоминания отключены для чата {}", group.getChatId());
                        continue;
                    }

                    // Получаем расписание для текущего дня и типа недели
                    List<Schedule> todaySchedule = scheduleService.findEntitiesByDay(currentDayOfWeek);

                    if (todaySchedule.isEmpty()) {
                        log.debug("📭 Нет пар на сегодня для чата {}", group.getChatId());
                        continue;
                    }

                    log.debug("📅 Для чата {} найдено {} пар на сегодня",
                            group.getChatId(), todaySchedule.size());

                    for (Schedule schedule : todaySchedule) {
                        // Проверяем тип недели
                        String scheduleWeekType = schedule.getWeekType() != null ? schedule.getWeekType() : "all";
                        if (!scheduleWeekType.equals(currentWeekType) && !scheduleWeekType.equals("all")) {
                            continue;
                        }

                        // Проверяем время начала пары
                        LocalTime classStartTime = schedule.getTimeStart();

                        // Округляем до минут для сравнения
                        LocalTime roundedCheckTime = LocalTime.of(checkTime.getHour(), checkTime.getMinute());
                        LocalTime roundedClassStart = LocalTime.of(classStartTime.getHour(), classStartTime.getMinute());

                        if (roundedCheckTime.equals(roundedClassStart)) {
                            // Отправляем напоминание с использованием НОВОЙ сигнатуры
                            reminderMessageService.sendClassReminder(
                                    group.getChatId(),
                                    schedule,  // Передаем объект Schedule, а не строку
                                    minutesBefore  // Передаем количество минут
                            );

                            log.info("✅ Напоминание отправлено в чат {}: '{}' в {} (за {} минут)",
                                    group.getChatId(),
                                    schedule.getSubject(),
                                    classStartTime.format(TIME_FORMATTER),
                                    minutesBefore);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Ошибка при проверке напоминаний для чата {}: {}",
                            group.getChatId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при проверке напоминаний о парах: {}", e.getMessage(), e);
        }
    }

    /**
     * Тестовая отправка для проверки (можно вызывать через API)
     */
    public void sendTestScheduleNow() {
        log.info("🧪 Запуск тестовой отправки расписания...");
        reminderMessageService.sendDailyScheduleToAllChats();
    }

    /**
     * Тестовая отправка дедлайнов для проверки
     */
    public void sendTestDeadlinesNow() {
        log.info("🧪 Запуск тестовой отправки дедлайнов...");
        reminderMessageService.sendWeeklyDeadlinesToAllChats();
    }

    /**
     * Тестовая отправка в конкретный чат
     */
    public void sendTestToChat(Long chatId, String messageType) {
        log.info("🧪 Тестовая отправка в чат {}: {}", chatId, messageType);
        reminderMessageService.sendTestMessageToChat(chatId, messageType);
    }
}