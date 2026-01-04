package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.repository.BotChatRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotChatService {

    private final BotChatRepository botChatRepository;

    @Transactional
    public BotChat registerOrUpdateChat(Chat telegramChat, Long userId) {
        Optional<BotChat> existing = botChatRepository.findByChatId(telegramChat.getId());

        BotChat chat;
        boolean isNew = false;

        if (existing.isPresent()) {
            chat = existing.get();
            chat.setTitle(telegramChat.getTitle());
            chat.setUsername(telegramChat.getUserName());
            chat.setUpdatedAt(LocalDateTime.now());
            chat.setIsActive(true);
        } else {
            isNew = true;
            chat = BotChat.builder()
                    .chatId(telegramChat.getId())
                    .chatType(telegramChat.getType())
                    .title(telegramChat.getTitle())
                    .username(telegramChat.getUserName())
                    .isActive(true)
                    .settings(createDefaultSettings(telegramChat.getType()))
                    .build();

            log.info("Зарегистрирован новый чат: {} ({})",
                    telegramChat.getId(),
                    telegramChat.getTitle() != null ? telegramChat.getTitle() : telegramChat.getUserName());
        }

        chat = botChatRepository.save(chat);

        // Инициализируем умные напоминания для новых групп (можно сделать позже)
        if (isNew && (telegramChat.isGroupChat() || telegramChat.isSuperGroupChat()) && userId != null) {
            // smartReminderService.initializeSmartReminders(chat.getChatId(), userId);
            log.info("Чат {} готов к использованию напоминаний", chat.getChatId());
        }

        return chat;
    }

    private Map<String, Object> createDefaultSettings(String chatType) {
        Map<String, Object> settings = new HashMap<>();

        if ("GROUP".equals(chatType) || "SUPERGROUP".equals(chatType)) {
            settings.put("schedule_notifications", true);
            settings.put("deadline_notifications", true);
            settings.put("reminder_before_class", 15); // За 15 минут до пары
            settings.put("welcome_message", true);
            settings.put("mention_all_enabled", true);
        } else {
            settings.put("schedule_notifications", false);
            settings.put("deadline_notifications", true);
            settings.put("reminder_before_class", 0);
        }

        return settings;
    }

    public Optional<BotChat> findByChatId(Long chatId) {
        return botChatRepository.findByChatId(chatId);
    }

    public void updateChatSettings(Long chatId, Map<String, Object> newSettings) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            Map<String, Object> currentSettings = chat.getSettings();
            if (currentSettings == null) {
                currentSettings = new HashMap<>();
            }
            currentSettings.putAll(newSettings);
            chat.setSettings(currentSettings);
            chat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(chat);
        });
    }

    @Transactional
    public void updateReminderBeforeClass(Long chatId, int minutes) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            Map<String, Object> settings = chat.getSettings();
            if (settings == null) {
                settings = new HashMap<>();
            }
            settings.put("reminder_before_class", minutes);
            chat.setSettings(settings);
            chat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(chat);

            log.info("Обновлено напоминание перед парой для чата {}: {} минут", chatId, minutes);
        });
    }

    @Transactional
    public void toggleScheduleNotifications(Long chatId, boolean enable) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            Map<String, Object> settings = chat.getSettings();
            if (settings == null) {
                settings = new HashMap<>();
            }
            settings.put("schedule_notifications", enable);
            chat.setSettings(settings);
            chat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(chat);

            log.info("{} уведомления о расписании для чата {}",
                    enable ? "Включены" : "Выключены", chatId);
        });
    }

    @Transactional
    public void toggleDeadlineNotifications(Long chatId, boolean enable) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            Map<String, Object> settings = chat.getSettings();
            if (settings == null) {
                settings = new HashMap<>();
            }
            settings.put("deadline_notifications", enable);
            chat.setSettings(settings);
            chat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(chat);

            log.info("{} уведомления о дедлайнах для чата {}",
                    enable ? "Включены" : "Выключены", chatId);
        });
    }
}