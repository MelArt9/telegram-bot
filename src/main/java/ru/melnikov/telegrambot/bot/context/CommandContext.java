package ru.melnikov.telegrambot.bot.context;

import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Getter
@Builder
public class CommandContext {

    private final Update update;
    private final Long chatId;
    private final User user;
    private final String text;
    private final String[] args;

    public String arg(int index) {
        return (args != null && args.length > index) ? args[index] : null;
    }
}