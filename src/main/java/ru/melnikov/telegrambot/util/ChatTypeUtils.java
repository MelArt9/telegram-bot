// Файл: /src/main/java/ru/melnikov/telegrambot/util/ChatTypeUtils.java
package ru.melnikov.telegrambot.util;

import org.telegram.telegrambots.meta.api.objects.Chat;

public class ChatTypeUtils {

    private ChatTypeUtils() {
        // Утилитный класс
    }

    public static boolean isPrivateChat(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "private".equalsIgnoreCase(type);
    }

    public static boolean isGroupChat(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "group".equalsIgnoreCase(type) || "supergroup".equalsIgnoreCase(type);
    }

    public static boolean isSuperGroup(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "supergroup".equalsIgnoreCase(type);
    }

    public static boolean isChannel(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "channel".equalsIgnoreCase(type);
    }
}