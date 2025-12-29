package ru.melnikov.telegrambot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.melnikov.telegrambot.bot.TelegramBot;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void registerBot() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBot);
    }
}