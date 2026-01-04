package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.melnikov.telegrambot.config.BotConfig;
import ru.melnikov.telegrambot.repository.BotChatRepository;
import ru.melnikov.telegrambot.service.BotChatService;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final CommandRouter router;
    private final BotChatService botChatService;
    private final BotChatRepository botChatRepository;

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Обработка добавления бота в группу
            if (update.hasMyChatMember()) {
                handleChatMemberUpdate(update.getMyChatMember());
                return;
            }

            // Обработка обычных сообщений
            if (update.hasMessage()) {
                Long userId = update.getMessage().getFrom().getId();

                // Регистрируем/обновляем чат
                botChatService.registerOrUpdateChat(update.getMessage().getChat(), userId);

                // Обработка команд
                SendMessage message = router.route(update);
                if (message != null) {
                    execute(message);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка обработки апдейта", e);
        }
    }

    private void handleChatMemberUpdate(ChatMemberUpdated chatMember) {
        try {
            var chat = chatMember.getChat();
            var newStatus = chatMember.getNewChatMember().getStatus();

            log.info("Изменение статуса бота в чате {}: {} -> {}",
                    chat.getId(),
                    chatMember.getOldChatMember().getStatus(),
                    newStatus);

            if ("member".equals(newStatus) || "administrator".equals(newStatus)) {
                // Бота добавили в группу
                Long userId = chatMember.getFrom().getId();
                botChatService.registerOrUpdateChat(chat, userId);

                // Отправляем приветственное сообщение если включено
                botChatService.findByChatId(chat.getId()).ifPresent(botChat -> {
                    Map<String, Object> settings = botChat.getSettings();
                    if (settings != null && (boolean) settings.getOrDefault("welcome_message", true)) {
                        sendWelcomeMessage(chat.getId(), chat.getTitle());
                    }
                });
            } else if ("kicked".equals(newStatus) || "left".equals(newStatus)) {
                // Бота удалили из группы - деактивируем чат
                botChatService.findByChatId(chat.getId()).ifPresent(botChat -> {
                    botChat.setIsActive(false);
                    botChat.setUpdatedAt(LocalDateTime.now());
                    botChatRepository.save(botChat);
                });
            }
        } catch (Exception e) {
            log.error("Ошибка обработки обновления участника чата", e);
        }
    }

    private void sendWelcomeMessage(Long chatId, String chatTitle) {
        try {
            String welcomeMessage = String.format("""
                👋 *Приветствуем в группе «%s»!*
                
                🤖 *Я — умный учебный помощник с автоматическими напоминаниями*
                
                📅 *Автоматические функции:*
                • Ежедневное расписание в 08:00
                • Напоминание перед каждой парой
                • Еженедельные дедлайны (Вт, Чт, Пт)
                
                🔧 *Команды управления:*
                • /reminders – настройка напоминаний
                • /settings – настройки группы
                • /today – расписание на сегодня
                • /deadlines – все дедлайны
                
                💡 *Все данные берутся из учебной базы*
                *Напоминания работают автоматически!*
                """, chatTitle != null ? chatTitle : "группа");

            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(welcomeMessage)
                    .parseMode("Markdown")
                    .build();

            execute(message);
        } catch (Exception e) {
            log.error("Ошибка отправки приветственного сообщения", e);
        }
    }
}