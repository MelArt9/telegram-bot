package ru.melnikov.telegrambot.bot;

import java.util.Arrays;

public enum CommandType {
    START("/start", "Начать работу"),
    TODAY("/today", "Расписание на сегодня"),
    DAY("/day", "Расписание по дню"),
    WEEK("/week", "Расписание на неделю"), // НОВАЯ КОМАНДА
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
        if (text == null || text.trim().isEmpty()) return UNKNOWN;

        String trimmed = text.trim();

        // Обработка кнопок
        if (trimmed.equals("📅 Сегодня")) return TODAY;
        if (trimmed.equals("⏰ Дедлайны")) return DEADLINES;
        if (trimmed.equals("🔗 Ссылки")) return LINKS;
        if (trimmed.equals("👥 Упомянуть всех")) return TAG;
        if (trimmed.equals("❓ Помощь")) return HELP;

        // Обработка обычных команд
        return Arrays.stream(values())
                .filter(c -> !c.command.isEmpty() && trimmed.startsWith(c.command))
                .findFirst()
                .orElse(UNKNOWN);
    }
}