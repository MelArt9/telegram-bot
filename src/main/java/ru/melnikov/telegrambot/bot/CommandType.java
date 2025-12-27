package ru.melnikov.telegrambot.bot;

import java.util.Arrays;

public enum CommandType {

    START("/start"),
    TODAY("/today"),
    DAY("/day"),
    DEADLINES("/deadlines"),
    LINKS("/links"),
    TAG("/tag"),
    UNKNOWN("");

    private final String command;

    CommandType(String command) {
        this.command = command;
    }

    public static CommandType fromText(String text) {
        if (text == null || text.isBlank()) return UNKNOWN;

        return Arrays.stream(values())
                .filter(c -> text.startsWith(c.command))
                .findFirst()
                .orElse(UNKNOWN);
    }
}