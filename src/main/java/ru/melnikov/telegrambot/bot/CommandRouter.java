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
        if (update == null || update.getMessage() == null) return null;

        String text = update.getMessage().getText();
        if (text == null) return null;

        CommandType command = CommandType.fromText(text);

        CommandContext context = CommandContext.builder()
                .update(update)
                .chatId(update.getMessage().getChatId())
                .user(update.getMessage().getFrom())
                .text(text)
                .args(text.split("\\s+"))
                .build();

        return commandService.handle(command, context);
    }
}