package ru.melnikov.telegrambot.bot.context;

import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Update;

@Getter
@Builder
public class CommandContext {
    private final Update update;
    private final Long chatId;
    private final org.telegram.telegrambots.meta.api.objects.User user;
    private final String text;
    private final String[] args;

    public String arg(int index) {
        return (args != null && args.length > index) ? args[index] : null;
    }

    @Builder.Default
    private final Integer messageThreadId = null; // ID темы
}