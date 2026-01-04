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

    public void sendMessage(Long topicId, String text) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(topicId.toString())
                .text(text)
                .parseMode(org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWN)
                .build();

        telegramBot.execute(message);
    }

    public void sendMessageWithParseMode(Long topicId, String text, String parseMode) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(topicId.toString())
                .text(text)
                .parseMode(parseMode)
                .build();

        telegramBot.execute(message);
    }
}