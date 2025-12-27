package ru.melnikov.telegrambot.bot;

public record ParsedCommand(
        CommandType type,
        String[] args
) {
}