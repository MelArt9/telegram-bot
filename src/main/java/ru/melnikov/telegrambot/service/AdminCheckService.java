package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.config.AdminConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCheckService {

    private final AdminConfig adminConfig;

    /**
     * Проверяет, является ли пользователь администратором
     */
    public boolean isAdmin(String username, Long userId) {
        // Проверяем по username
        boolean isAdminByUsername = username != null &&
                adminConfig.isAdminByUsername(username);

        // Проверяем по userId
        boolean isAdminByUserId = userId != null &&
                adminConfig.isAdminByUserId(userId);

        boolean isAdmin = isAdminByUsername || isAdminByUserId;

        log.debug("Проверка администратора: username={}, userId={}, результат={}",
                username, userId, isAdmin);

        return isAdmin;
    }

    /**
     * Проверяет, является ли команда админской
     */
    public boolean isAdminCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        // Список админских команд
        return command.startsWith("/reminders") ||
                command.startsWith("/settings") ||
                command.startsWith("/admin"); // если будут другие админские команды
    }
}