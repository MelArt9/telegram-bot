package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.melnikov.telegrambot.bot.CommandService;

@Component
@RequiredArgsConstructor
public class CommandRouter {

    private final CommandService commandService;

    public SendMessage route(Update update) {
        if (update.getMessage() == null || update.getMessage().getText() == null) {
            return null;
        }

        String text = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        CommandType type = CommandType.fromText(text);

        return switch (type) {
            case START -> commandService.start(chatId, update.getMessage().getFrom());
            case TODAY -> commandService.today(chatId);
            case DEADLINES -> commandService.deadlines(chatId);
            case LINKS -> commandService.links(chatId);
            case DAY -> commandService.day(chatId, text);
            case TAG -> commandService.tag(chatId, text);
            default -> commandService.unknown(chatId);
        };
    }
}