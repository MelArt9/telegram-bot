package ru.melnikov.telegrambot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "telegram.admins")
public class AdminConfig {

    /**
     * Список username администраторов (без @)
     */
    private List<String> usernames = new ArrayList<>();

    /**
     * Список ID пользователей администраторов
     */
    private List<Long> userIds = new ArrayList<>();

    /**
     * Проверяет, является ли username администратором
     */
    public boolean isAdminByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        // Убираем @ если есть
        String cleanUsername = username.startsWith("@") ?
                username.substring(1) : username;
        return usernames.stream()
                .anyMatch(admin -> admin.equalsIgnoreCase(cleanUsername));
    }

    /**
     * Проверяет, является ли userId администратором
     */
    public boolean isAdminByUserId(Long userId) {
        return userId != null && userIds.contains(userId);
    }

    /**
     * Универсальная проверка администратора (по username или userId)
     */
    public boolean isAdmin(String username, Long userId) {
        return isAdminByUsername(username) || isAdminByUserId(userId);
    }
}