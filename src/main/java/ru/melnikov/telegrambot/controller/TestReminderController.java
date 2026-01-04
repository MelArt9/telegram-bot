// Файл: /src/main/java/ru/melnikov/telegrambot/controller/TestReminderController.java
package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.melnikov.telegrambot.service.ReminderMessageService;
import ru.melnikov.telegrambot.service.SmartReminderService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestReminderController {

    private final SmartReminderService smartReminderService;
    private final ReminderMessageService reminderMessageService;

    @GetMapping("/api/test/send-schedule")
    public Map<String, Object> testSendSchedule() {
        Map<String, Object> response = new HashMap<>();

        try {
            smartReminderService.sendTestScheduleNow();
            response.put("status", "success");
            response.put("message", "Тестовая отправка расписания запущена");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            log.error("Ошибка тестовой отправки: ", e);
        }

        return response;
    }

    @GetMapping("/api/test/send-deadlines")
    public Map<String, Object> testSendDeadlines() {
        Map<String, Object> response = new HashMap<>();

        try {
            smartReminderService.sendTestDeadlinesNow();
            response.put("status", "success");
            response.put("message", "Тестовая отправка дедлайнов запущена");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            log.error("Ошибка тестовой отправки: ", e);
        }

        return response;
    }

    @GetMapping("/api/test/send-to-chat/{chatId}")
    public Map<String, Object> testSendToChat(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "schedule") String type) {

        Map<String, Object> response = new HashMap<>();

        try {
            if ("schedule".equalsIgnoreCase(type)) {
                reminderMessageService.sendTestScheduleToChat(chatId);
                response.put("message", "Расписание отправлено в чат " + chatId);
            } else if ("deadlines".equalsIgnoreCase(type)) {
                reminderMessageService.sendTestDeadlinesToChat(chatId);
                response.put("message", "Дедлайны отправлены в чат " + chatId);
            } else {
                response.put("error", "Неизвестный тип: " + type + " (используйте schedule или deadlines)");
                return response;
            }

            response.put("status", "success");
            response.put("chatId", chatId);
            response.put("type", type);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            log.error("Ошибка отправки в чат {}: ", chatId, e);
        }

        return response;
    }

    @GetMapping("/api/test/reminders-status")
    public Map<String, Object> getRemindersStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "running");
            response.put("time", java.time.LocalTime.now().toString());
            response.put("bot", "online");
            response.put("scheduler", "active");

            // Добавьте здесь проверку других компонентов

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
}