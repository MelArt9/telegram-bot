// Файл: /src/main/java/ru/melnikov/telegrambot/controller/ReminderSettingsController.java
package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.melnikov.telegrambot.service.SmartReminderService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReminderSettingsController {

    private final SmartReminderService smartReminderService;

    @GetMapping("/api/reminders/settings")
    public Map<String, Object> getReminderSettings() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Получаем информацию о настройках
            String settingsInfo = smartReminderService.getCurrentSettingsInfo();
            Map<String, Object> todayStatus = smartReminderService.getTodaySendStatus();

            response.put("success", true);
            response.put("settingsInfo", settingsInfo);
            response.put("todayStatus", todayStatus);
            response.put("currentTime", java.time.LocalTime.now().toString());
            response.put("currentDay", java.time.LocalDate.now().getDayOfWeek().toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("Ошибка получения настроек: ", e);
        }

        return response;
    }

    @GetMapping("/api/reminders/test-now")
    public Map<String, Object> testRemindersNow() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Запускаем тестовую отправку
            smartReminderService.sendTestScheduleNow();
            smartReminderService.sendTestDeadlinesNow();

            response.put("success", true);
            response.put("message", "Тестовая отправка запущена");
            response.put("time", java.time.LocalTime.now().toString());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("Ошибка тестовой отправки: ", e);
        }

        return response;
    }
}