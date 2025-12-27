package ru.melnikov.telegrambot.bot;

import java.util.Arrays;

public enum CommandType {

    START("/start"),
    TODAY("/today"),
    DEADLINES("/deadlines"),
    LINKS("/links"),
    TAG("/tag"),
    DAY("/day"),
    UNKNOWN("");

    private final String command;

    CommandType(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static CommandType fromText(String text) {
        if (text == null) return UNKNOWN;
        return Arrays.stream(values())
                .filter(cmd -> text.startsWith(cmd.command))
                .findFirst()
                .orElse(UNKNOWN);
    }
}