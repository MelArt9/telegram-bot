package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.melnikov.telegrambot.config.BotSettingsConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotSettingsConfig settingsConfig;
    private final CommandRouter router;

    @Override
    public String getBotUsername() {
        return settingsConfig.getBot().getUsername();
    }

    @Override
    public String getBotToken() {
        return settingsConfig.getBot().getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Логика обработки...
            SendMessage message = router.route(update);
            if (message != null) {
                execute(message);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки апдейта: {}", e.getMessage(), e);
        }
    }
}