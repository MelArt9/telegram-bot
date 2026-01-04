package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.melnikov.telegrambot.bot.TelegramBot;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiService {

    private final TelegramBot telegramBot;

    /**
     * Проверяет, является ли пользователь администратором группы
     */
    public boolean isGroupAdmin(Long chatId, Long userId) {
        try {
            GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
            getChatAdministrators.setChatId(chatId.toString());

            List<ChatMember> administrators;
            try {
                administrators = telegramBot.execute(getChatAdministrators);
            } catch (TelegramApiException e) {
                log.error("Ошибка получения списка администраторов для чата {}: {}", chatId, e.getMessage());
                return false;
            }

            return administrators.stream()
                    .anyMatch(admin -> admin.getUser().getId().equals(userId));

        } catch (Exception e) {
            log.error("Ошибка проверки прав администратора: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, является ли пользователь администратором или создателем группы
     */
    public boolean isGroupAdminOrCreator(Long chatId, Long userId) {
        try {
            GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
            getChatAdministrators.setChatId(chatId.toString());

            List<ChatMember> administrators;
            try {
                administrators = telegramBot.execute(getChatAdministrators);
            } catch (TelegramApiException e) {
                log.error("Ошибка получения списка администраторов: {}", e.getMessage());
                return false;
            }

            return administrators.stream()
                    .anyMatch(admin ->
                            admin.getUser().getId().equals(userId) &&
                                    ("creator".equals(admin.getStatus()) || "administrator".equals(admin.getStatus()))
                    );

        } catch (Exception e) {
            log.error("Ошибка проверки прав администратора/создателя: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Упрощенная проверка - только для отладки
     */
    public boolean isGroupAdminSimple(Long chatId, Long userId) {
        // Временно возвращаем true для тестирования
        // TODO: Заменить на реальную проверку
        log.warn("Используется упрощенная проверка администратора для userId: {}", userId);
        return true;
    }
}