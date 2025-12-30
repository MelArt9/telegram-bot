package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.melnikov.telegrambot.bot.context.CommandContext;

@Component
@RequiredArgsConstructor
public class CommandRouter {

    private final CommandService commandService;

    public SendMessage route(Update update) {
        if (update == null) return null;

        // Обработка сообщений
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            // Маппинг кнопок на команды
            String commandText = mapButtonToCommand(text);
            CommandType type = CommandType.fromText(commandText);

            CommandContext ctx = CommandContext.builder()
                    .update(update)
                    .chatId(chatId)
                    .user(update.getMessage().getFrom())
                    .text(text)
                    .args(parseArgs(commandText))
                    .build();

            return commandService.handle(type, ctx);
        }

        return null;
    }

    private String mapButtonToCommand(String text) {
        return switch (text) {
            case "📅 Сегодня" -> "/today";
            case "⏰ Дедлайны" -> "/deadlines";
            case "🔗 Ссылки" -> "/links";
            case "👥 Упомянуть всех" -> "/tag all";
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