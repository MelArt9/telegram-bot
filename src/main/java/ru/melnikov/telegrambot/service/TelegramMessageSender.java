package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.melnikov.telegrambot.bot.TelegramBot;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageSender {

    private final TelegramBot telegramBot;

    public void sendMessage(Long chatId, String text, String parseMode) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode(parseMode)
                    .build();

            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в чат {}: {}", chatId, e.getMessage());
        }
    }

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMarkdownMessage(Long chatId, String text) {
        sendMessage(chatId, text, "Markdown");
    }
}