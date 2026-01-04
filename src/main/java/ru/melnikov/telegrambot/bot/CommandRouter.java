package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.melnikov.telegrambot.bot.context.CommandContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandRouter {

    private final CommandService commandService;

    public SendMessage route(Update update) {
        if (update == null) return null;

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Chat chat = update.getMessage().getChat();

            // Определяем тип чата
            boolean isGroupChat = isGroupChat(chat);

            // ВАЖНОЕ ИЗМЕНЕНИЕ: В чатах реагируем только на команды с /
            if (isGroupChat && !text.startsWith("/")) {
                log.info("Игнорируем некомандное сообщение в чате {} (тип: {}): '{}'",
                        chatId, chat.getType(), text);
                return null;
            }

            // ВАЖНО: Получаем ID темы из сообщения
            Integer messageThreadId = update.getMessage().getMessageThreadId();

            String commandText = mapButtonToCommand(text);
            CommandType type = CommandType.fromText(commandText);

            CommandContext ctx = CommandContext.builder()
                    .update(update)
                    .chatId(chatId)
                    .user(update.getMessage().getFrom())
                    .text(text)
                    .args(parseArgs(commandText))
                    .messageThreadId(messageThreadId) // ← ПЕРЕДАЕМ ID ТЕМЫ!
                    .build();

            return commandService.handle(type, ctx);
        }

        return null;
    }

    // Метод для определения, является ли чат группой
    private boolean isGroupChat(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "group".equals(type) || "supergroup".equals(type) ||
                "GROUP".equals(type) || "SUPERGROUP".equals(type);
    }

    // В методе mapButtonToCommand оставляем как было
    private String mapButtonToCommand(String text) {
        return switch (text) {
            case "📅 Сегодня" -> "/today";
            case "⏰ Дедлайны" -> "/deadlines";
            case "🔗 Ссылки" -> "/links";
            case "👥 Упомянуть всех" -> "/tag all";
            case "🔔 Напоминания" -> "/reminders";
            case "⚙️ Настройки" -> "/settings";
            case "🛡️ Администратор" -> "/admin";
            case "❓ Помощь" -> "/help";
            default -> text;
        };
    }

    private String[] parseArgs(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split("\\s+");
    }
}