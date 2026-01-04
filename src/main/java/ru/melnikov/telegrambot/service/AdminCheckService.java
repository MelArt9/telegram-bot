package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.config.BotSettingsConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCheckService {

    private final BotSettingsConfig settingsConfig;

    /**
     * Проверяет, является ли пользователь администратором
     */
    public boolean isAdmin(String username, Long userId) {
        return settingsConfig.getAdmins().isAdmin(username, userId);
    }
}