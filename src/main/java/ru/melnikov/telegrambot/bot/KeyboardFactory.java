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
                KeyboardButton.builder().text("📅 Сегодня").build(),
                KeyboardButton.builder().text("⏰ Дедлайны").build()
        ));

        KeyboardRow row2 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("🔗 Ссылки").build(),
                KeyboardButton.builder().text("👥 Упомянуть всех").build()
        ));

        KeyboardRow row3 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("🔔 Напоминания").build(),
                KeyboardButton.builder().text("❓ Помощь").build()
        ));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3));
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        markup.setOneTimeKeyboard(false);
        return markup;
    }

    // Дополнительная клавиатура для групп
    public ReplyKeyboardMarkup groupKeyboard() {
        KeyboardRow row1 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("📅 Сегодня").build(),
                KeyboardButton.builder().text("⏰ Дедлайны").build()
        ));

        KeyboardRow row2 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("🔔 Напоминания").build(),
                KeyboardButton.builder().text("⚙️ Настройки").build()
        ));

        KeyboardRow row3 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("👥 Упомянуть всех").build(),
                KeyboardButton.builder().text("❓ Помощь").build()
        ));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3));
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        markup.setOneTimeKeyboard(false);
        return markup;
    }

    // Минимальная клавиатура для чатов (только основные команды)
    public ReplyKeyboardMarkup minimalKeyboard() {
        // Вариант 1: Показываем только /help
        KeyboardRow row = new KeyboardRow(List.of(
                KeyboardButton.builder().text("/help").build(),
                KeyboardButton.builder().text("/today").build()
        ));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        markup.setOneTimeKeyboard(false);
        return markup;

        // Вариант 2: Убираем клавиатуру совсем
        // return null;

        // Вариант 3: Используем ReplyKeyboardRemove для скрытия клавиатуры
        // return new ReplyKeyboardRemove(true);
    }
}