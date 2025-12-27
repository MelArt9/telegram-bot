package ru.melnikov.telegrambot.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Component
public class KeyboardFactory {

    public ReplyKeyboardMarkup defaultKeyboard() {
        KeyboardRow row1 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("/today").build(),
                KeyboardButton.builder().text("/deadlines").build()
        ));

        KeyboardRow row2 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("/links").build(),
                KeyboardButton.builder().text("/tag all").build()
        ));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        return markup;
    }
}
