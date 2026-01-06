// Файл: /src/main/java/ru/melnikov/telegrambot/bot/TelegramBot.java
package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.service.ChatEventService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotSettingsConfig settingsConfig;
    private final CommandRouter router;
    private final ChatEventService chatEventService;

    @Override
    public String getBotUsername() {
        return settingsConfig.getBot().getUsername();
    }

    @Override
    public String getBotToken() {
        return settingsConfig.getBot().getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // 1. Обработка событий чата (добавление/удаление бота)
            handleChatEvents(update);

            // 2. Обработка обычных команд
            if (update.hasMessage() && update.getMessage().hasText()) {
                SendMessage message = router.route(update);
                if (message != null) {
                    execute(message);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка обработки апдейта: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка событий чата
     */
    private void handleChatEvents(Update update) {
        try {
            // Обработка добавления бота в группу
            if (update.hasMessage() && update.getMessage().getNewChatMembers() != null) {
                for (User newMember : update.getMessage().getNewChatMembers()) {
                    if (newMember.getIsBot() != null && newMember.getIsBot()) {
                        try {
                            User me = getMe();
                            if (me != null && newMember.getId().equals(me.getId())) {
                                // Бота добавили в чат
                                chatEventService.handleBotAddedToChat(
                                        update.getMessage().getChat(),
                                        update.getMessage().getFrom()
                                );

                                // Автоматически предполагаем, что если бота добавили в группу,
                                // ему дадут права администратора (нужно будет подтвердить)
                                // Можно отправить сообщение с инструкцией
                                sendAdminInstructions(
                                        update.getMessage().getChat().getId(),
                                        update.getMessage().getChat().getTitle()
                                );

                                break;
                            }
                        } catch (Exception e) {
                            log.error("Ошибка при получении информации о боте: {}", e.getMessage());
                        }
                    }
                }
            }

            // Обработка удаления бота из группы
            if (update.hasMessage() && update.getMessage().getLeftChatMember() != null) {
                User leftMember = update.getMessage().getLeftChatMember();
                if (leftMember.getIsBot() != null && leftMember.getIsBot()) {
                    try {
                        User me = getMe();
                        if (me != null && leftMember.getId().equals(me.getId())) {
                            // Бота удалили из чата
                            chatEventService.handleBotRemovedFromChat(
                                    update.getMessage().getChat().getId()
                            );
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при проверке левого участника: {}", e.getMessage());
                    }
                }
            }

            // Обработка команды для установки админского статуса
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                if (text.startsWith("/setadmin") || text.startsWith("/iamadmin")) {
                    handleAdminStatusCommand(update);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка обработки события чата: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка команды установки админского статуса
     */
    private void handleAdminStatusCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = update.getMessage().getFrom();

        // Проверяем, является ли пользователь администратором бота
        boolean isBotAdmin = settingsConfig.getAdmins().isAdmin(
                user.getUserName(),
                user.getId()
        );

        if (isBotAdmin) {
            // Устанавливаем статус администратора для бота в этом чате
            chatEventService.setBotAdminStatus(chatId, true);

            try {
                SendMessage reply = SendMessage.builder()
                        .chatId(chatId)
                        .text("✅ *Бот установлен как администратор в этом чате!*\n\n" +
                                "Теперь можно использовать команды:\n" +
                                "• `/settopic` - установить тему для бота\n" +
                                "• `/reminders` - настроить напоминания\n" +
                                "• `/chatinfo` - информация о чате")
                        .parseMode("Markdown")
                        .build();
                execute(reply);
            } catch (Exception e) {
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
        } else {
            try {
                SendMessage reply = SendMessage.builder()
                        .chatId(chatId)
                        .text("❌ *Только администраторы бота могут использовать эту команду*")
                        .parseMode("Markdown")
                        .build();
                execute(reply);
            } catch (Exception e) {
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
        }
    }

    /**
     * Отправка инструкций по выдаче прав администратора
     */
    private void sendAdminInstructions(Long chatId, String chatTitle) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text("👋 *Привет! Я добавлен в группу \"" + (chatTitle != null ? chatTitle : "эту группу") + "\"*\n\n" +
                            "⚠️ *Для полноценной работы мне нужны права администратора:*\n\n" +
                            "1. *Настройте бота как администратора* в настройках группы\n" +
                            "2. *Выдайте следующие права:*\n" +
                            "   • Отправка сообщений\n" +
                            "   • Управление темами (для форумов)\n" +
                            "   • Закрепление сообщений\n\n" +
                            "3. *После выдачи прав напишите команду:*\n" +
                            "   `/iamadmin`\n\n" +
                            "📋 *После этого я смогу:*\n" +
                            "• Автоматически отправлять расписание\n" +
                            "• Напоминать о дедлайнах\n" +
                            "• Работать в определенной теме\n" +
                            "• И многое другое!")
                    .parseMode("Markdown")
                    .build();
            execute(message);
        } catch (Exception e) {
            log.error("Ошибка отправки инструкций: {}", e.getMessage());
        }
    }
}