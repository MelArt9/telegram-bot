// Файл: /src/main/java/ru/melnikov/telegrambot/controller/ChatAdminController.java
package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.repository.BotChatRepository;
import ru.melnikov.telegrambot.service.ChatEventService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatAdminController {

    private final BotChatRepository botChatRepository;
    private final ChatEventService chatEventService;

    /**
     * Получить все чаты
     */
    @GetMapping
    public ResponseEntity<List<BotChat>> getAllChats() {
        return ResponseEntity.ok(botChatRepository.findAll());
    }

    /**
     * Получить активные чаты
     */
    @GetMapping("/active")
    public ResponseEntity<List<BotChat>> getActiveChats() {
        return ResponseEntity.ok(botChatRepository.findByIsActiveTrue());
    }

    /**
     * Получить чаты, где бот администратор
     */
    @GetMapping("/admin")
    public ResponseEntity<List<BotChat>> getAdminChats() {
        // Нужно будет добавить метод в репозиторий
        List<BotChat> allChats = botChatRepository.findAll();
        List<BotChat> adminChats = allChats.stream()
                .filter(chat -> Boolean.TRUE.equals(chat.getIsBotAdmin()))
                .toList();
        return ResponseEntity.ok(adminChats);
    }

    /**
     * Обновить статус конкретного чата
     */
    @PostMapping("/{chatId}/check")
    public ResponseEntity<Map<String, Object>> checkChatStatus(@PathVariable Long chatId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String chatInfo = chatEventService.getChatInfo(chatId);
            response.put("success", true);
            response.put("chatInfo", chatInfo);
            response.put("chatId", chatId);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("chatId", chatId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Установить бота как администратора (ручная корректировка)
     */
    @PostMapping("/{chatId}/set-admin")
    public ResponseEntity<Map<String, Object>> setAsAdmin(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "true") boolean isAdmin) {

        Map<String, Object> response = new HashMap<>();

        botChatRepository.findByChatId(chatId).ifPresentOrElse(
                chat -> {
                    chat.setIsBotAdmin(isAdmin);
                    chat.setUpdatedAt(java.time.LocalDateTime.now());
                    botChatRepository.save(chat);

                    response.put("success", true);
                    response.put("message", "Статус администратора обновлен");
                    response.put("chatId", chatId);
                    response.put("isAdmin", isAdmin);

                    log.info("Статус администратора для чата {} установлен в {}",
                            chatId, isAdmin);
                },
                () -> {
                    response.put("success", false);
                    response.put("error", "Чат не найден");
                    response.put("chatId", chatId);
                }
        );

        return ResponseEntity.ok(response);
    }
}