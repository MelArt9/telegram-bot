// Файл: /src/main/java/ru/melnikov/telegrambot/service/ChatAdminChecker.java
package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.repository.BotChatRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAdminChecker {

    private final BotChatRepository botChatRepository;
    private final ChatEventService chatEventService;

    /**
     * Периодическая проверка статуса бота в чатах
     * Запускается каждые 10 минут
     */
    @Scheduled(fixedDelay = 600000) // 10 минут
    public void checkBotAdminStatus() {
        log.info("🔄 Запуск периодической проверки статуса бота в чатах...");

        List<BotChat> activeChats = botChatRepository.findByIsActiveTrue();
        log.info("Найдено активных чатов: {}", activeChats.size());

        for (BotChat chat : activeChats) {
            try {
                checkSingleChat(chat);
            } catch (Exception e) {
                log.error("Ошибка при проверке чата {}: {}", chat.getChatId(), e.getMessage());
            }
        }

        log.info("✅ Проверка статуса бота завершена");
    }

    private void checkSingleChat(BotChat chat) {
        // Пропускаем личные чаты
        if (!isGroupChat(chat.getChatType())) {
            return;
        }

        try {
            // В реальности здесь нужно вызывать getChatAdministrators API
            // Но так как у нас нет доступа к TelegramBot из сервиса,
            // эту логику нужно будет вынести в контроллер или использовать другой подход

            log.debug("Проверка чата {} (тип: {})", chat.getChatId(), chat.getChatType());

            // Для демо просто логируем
            if (chat.getIsBotAdmin() != null && chat.getIsBotAdmin()) {
                log.debug("Бот является администратором в чате {}", chat.getChatId());
            } else {
                log.debug("Бот НЕ является администратором в чате {}", chat.getChatId());
            }

        } catch (Exception e) {
            log.warn("Не удалось проверить чат {}: {}", chat.getChatId(), e.getMessage());

            // Если не удается проверить чат, возможно, бота удалили
            if (e.getMessage() != null &&
                    (e.getMessage().contains("chat not found") ||
                            e.getMessage().contains("bot was kicked"))) {
                chat.setIsActive(false);
                chat.setIsBotAdmin(false);
                botChatRepository.save(chat);
                log.info("Чат {} помечен как неактивный", chat.getChatId());
            }
        }
    }

    private boolean isGroupChat(String chatType) {
        if (chatType == null) return false;
        String type = chatType.toLowerCase();
        return type.contains("group") || type.contains("supergroup");
    }
}