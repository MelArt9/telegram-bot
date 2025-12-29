package ru.melnikov.telegrambot.bot;

import java.util.Arrays;

public enum CommandType {
    START("/start", "Начать работу"),
    TODAY("/today", "Расписание на сегодня"),
    DAY("/day", "Расписание по дню"),
    DEADLINES("/deadlines", "Ближайшие дедлайны"),
    LINKS("/links", "Полезные ссылки"),
    TAG("/tag", "Упомянуть группу"),
    HELP("/help", "Помощь"),
    UNKNOWN("", "");

    private final String command;
    private final String description;

    CommandType(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public static CommandType fromText(String text) {
        if (text == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(c -> !c.command.isEmpty() && text.startsWith(c.command))
                .findFirst()
                .orElse(UNKNOWN);
    }
}