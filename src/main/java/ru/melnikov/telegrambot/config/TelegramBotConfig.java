package ru.melnikov.telegrambot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.melnikov.telegrambot.bot.TelegramBot;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final TelegramBot telegramBot;
    private final BotSettingsConfig settingsConfig;

    @PostConstruct
    public void init() {
        // Проверяем конфигурацию
        if (!settingsConfig.getBot().isValid()) {
            log.error("❌ Конфигурация бота невалидна! Бот не будет зарегистрирован.");
            return;
        }

        if (!Boolean.TRUE.equals(settingsConfig.getBot().getEnabled())) {
            log.info("🤖 Бот отключен в настройках (telegram.bot.enabled=false)");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            log.info("✅ Telegram бот '@{}' успешно зарегистрирован и запущен!",
                    settingsConfig.getBot().getUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка при регистрации Telegram бота: {}", e.getMessage());
        }
    }
}