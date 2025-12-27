package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class BotCommandHandler {

    private final CommandRouter commandRouter;

    public SendMessage handle(Update update) {
        if (update == null || update.getMessage() == null) {
            return null;
        }
        return commandRouter.route(update);
    }
}