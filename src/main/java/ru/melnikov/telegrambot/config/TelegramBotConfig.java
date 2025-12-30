package ru.melnikov.telegrambot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.melnikov.telegrambot.bot.TelegramBot;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotConfig {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            log.info("✅ Telegram бот успешно зарегистрирован и запущен!");
        } catch (Exception e) {
            log.error("❌ Ошибка при регистрации Telegram бота: {}", e.getMessage());
            log.warn("Приложение продолжит работу без Telegram бота");
            // Не выбрасываем исключение, приложение продолжает работу
        }
    }
}