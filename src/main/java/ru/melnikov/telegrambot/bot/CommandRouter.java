package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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

    // В методе mapButtonToCommand добавляем:
    private String mapButtonToCommand(String text) {
        return switch (text) {
            case "📅 Сегодня" -> "/today";
            case "⏰ Дедлайны" -> "/deadlines";
            case "🔗 Ссылки" -> "/links";
            case "👥 Упомянуть всех" -> "/tag all";
            case "🔔 Напоминания" -> "/reminders";
            case "⚙️ Настройки" -> "/settings";
            case "\uD83D\uDEE1\uFE0F Администратор" -> "/admin";
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