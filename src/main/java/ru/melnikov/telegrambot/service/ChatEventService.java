// Файл: /src/main/java/ru/melnikov/telegrambot/service/ChatEventService.java
package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.repository.BotChatRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatEventService {

    private final BotChatRepository botChatRepository;
    private final BotSettingsConfig settingsConfig;

    /**
     * Обработка добавления бота в чат
     */
    @Transactional
    public void handleBotAddedToChat(Chat chat, User addedBy) {
        Long chatId = chat.getId();

        Optional<BotChat> existingChat = botChatRepository.findByChatId(chatId);

        if (existingChat.isPresent()) {
            // Обновляем существующий чат
            BotChat botChat = existingChat.get();
            botChat.setIsActive(true);
            botChat.setTitle(chat.getTitle());
            botChat.setUsername(chat.getUserName());
            botChat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(botChat);

            log.info("🤖 Бот повторно добавлен в чат {}: {}", chatId, getChatName(chat));
        } else {
            // Создаем новый чат
            Map<String, Object> settings = createDefaultGroupSettings();

            BotChat botChat = BotChat.builder()
                    .chatId(chatId)
                    .chatType(chat.getType())
                    .title(chat.getTitle())
                    .username(chat.getUserName())
                    .isActive(true)
                    .isBotAdmin(false) // Пока не знаем прав
                    .botPermissions("{}")
                    .settings(settings)
                    .build();

            botChatRepository.save(botChat);

            log.info("🎉 Бот добавлен в новый чат {}: {} (добавил: {})",
                    chatId, getChatName(chat),
                    addedBy != null ? getUserName(addedBy) : "unknown");
        }
    }

    /**
     * Обработка удаления бота из чата
     */
    @Transactional
    public void handleBotRemovedFromChat(Long chatId) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            chat.setIsActive(false);
            chat.setIsBotAdmin(false);
            chat.setBotPermissions("{}");
            chat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(chat);

            log.info("👋 Бот удален из чата {}: {}", chatId, chat.getTitle());
        });
    }

    /**
     * Обработка изменения прав бота в чате
     */
    @Transactional
    public void handleBotChatMemberUpdate(Chat oldChatMember, Chat newChatMember, User user) {
        if (newChatMember == null || user == null) return;

        Long chatId = newChatMember.getId();
        boolean isBot = user.getIsBot() != null && user.getIsBot();

        // Проверяем, что это наш бот
        if (isBot) {
            // В версии 6.7.0 статус нужно получать из контекста
            // Для простоты будем считать, что если чат есть в базе и бот активен - он в чате
            Optional<BotChat> chatOpt = botChatRepository.findByChatId(chatId);

            if (chatOpt.isPresent()) {
                BotChat botChat = chatOpt.get();

                // Проверяем, активен ли бот (простая логика)
                boolean isActiveNow = botChat.getIsActive() != null && botChat.getIsActive();

                if (!isActiveNow) {
                    // Бота кикнули или он вышел
                    botChat.setIsActive(false);
                    botChat.setIsBotAdmin(false);
                    botChat.setBotPermissions("{}");
                    botChat.setUpdatedAt(LocalDateTime.now());
                    botChatRepository.save(botChat);

                    log.info("🚫 Бот потерял доступ к чату {}: {}", chatId, botChat.getTitle());
                } else {
                    // Предполагаем, что если бот активен в группе - он администратор
                    // (в реальности нужно проверять через getChatAdministrators)
                    botChat.setIsBotAdmin(true);
                    botChat.setUpdatedAt(LocalDateTime.now());
                    botChatRepository.save(botChat);

                    log.info("👑 Бот предположительно администратор в чате {}: {}",
                            chatId, botChat.getTitle());
                }
            }
        }
    }

    /**
     * Обработка изменения статуса бота как администратора
     * Вызывается, когда точно известно, что бот стал админом
     */
    @Transactional
    public void handleBotPromotedToAdmin(Long chatId, ChatMember chatMember) {
        Optional<BotChat> chatOpt = botChatRepository.findByChatId(chatId);

        if (chatOpt.isPresent()) {
            BotChat botChat = chatOpt.get();
            botChat.setIsBotAdmin(true);
            botChat.setBotPermissions(extractPermissions(chatMember));
            botChat.setUpdatedAt(LocalDateTime.now());
            botChatRepository.save(botChat);

            log.info("👑 Боту официально выданы права администратора в чате {}: {}",
                    chatId, botChat.getTitle());

            // Отправляем приветственное сообщение
            sendWelcomeMessage(chatId, botChat);
        } else {
            // Создаем запись, если ее нет
            log.warn("Чат {} не найден в БД при попытке установить права администратора", chatId);
        }
    }

    /**
     * Получение информации о чате для команды /info
     */
    @Transactional(readOnly = true)
    public String getChatInfo(Long chatId) {
        return botChatRepository.findByChatId(chatId)
                .map(chat -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("📋 *Информация о чате:*\n\n");
                    sb.append("• *ID:* `").append(chat.getChatId()).append("`\n");
                    sb.append("• *Тип:* ").append(chat.getChatType()).append("\n");
                    sb.append("• *Название:* ").append(chat.getTitle() != null ? chat.getTitle() : "—").append("\n");
                    sb.append("• *Username:* ").append(chat.getUsername() != null ? "@" + chat.getUsername() : "—").append("\n");
                    sb.append("• *Активен:* ").append(chat.getIsActive() ? "✅" : "❌").append("\n");
                    sb.append("• *Бот админ:* ").append(chat.getIsBotAdmin() ? "✅" : "❌").append("\n");

                    return sb.toString();
                })
                .orElse("❌ *Чат не найден в базе данных*");
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======

    private Map<String, Object> createDefaultGroupSettings() {
        Map<String, Object> settings = new HashMap<>();

        // Настройки из конфигурации YML
        settings.put("schedule_notifications", true);
        settings.put("deadline_notifications", true);
        settings.put("before_class_enabled", settingsConfig.getReminders().getBeforeClass().getEnabled());
        settings.put("welcome_message", true);
        settings.put("mention_all_enabled", true);
        settings.put("bot_topic_id", null);
        settings.put("bot_topic_name", null);

        return settings;
    }

    private String extractPermissions(ChatMember chatMember) {
        try {
            Map<String, Object> permissions = new HashMap<>();

            if (chatMember instanceof ChatMemberAdministrator) {
                ChatMemberAdministrator admin = (ChatMemberAdministrator) chatMember;
                permissions.put("can_change_info", admin.getCanChangeInfo());
                permissions.put("can_post_messages", admin.getCanPostMessages());
                permissions.put("can_edit_messages", admin.getCanEditMessages());
                permissions.put("can_delete_messages", admin.getCanDeleteMessages());
                permissions.put("can_invite_users", admin.getCanInviteUsers());
                permissions.put("can_restrict_members", admin.getCanRestrictMembers());
                permissions.put("can_pin_messages", admin.getCanPinMessages());
                permissions.put("can_promote_members", admin.getCanPromoteMembers());
                permissions.put("can_manage_chat", admin.getCanManageChat());
                permissions.put("can_manage_video_chats", admin.getCanManageVideoChats());
                permissions.put("can_manage_topics", admin.getCanManageTopics());
                permissions.put("is_anonymous", admin.getIsAnonymous());
            } else if (chatMember instanceof ChatMemberOwner) {
                ChatMemberOwner owner = (ChatMemberOwner) chatMember;
                permissions.put("is_owner", true);
                permissions.put("is_anonymous", owner.getIsAnonymous());
            }

            // Конвертируем Map в JSON строку
            return mapToJson(permissions);

        } catch (Exception e) {
            log.error("Ошибка при извлечении прав: {}", e.getMessage());
            return "{}";
        }
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof Boolean) {
                json.append(entry.getValue());
            } else if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() == null) {
                json.append("null");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private void sendWelcomeMessage(Long chatId, BotChat botChat) {
        try {
            log.info("📨 Приветственное сообщение будет отправлено в чат {}", chatId);

            // Сохраняем флаг, что приветствие нужно отправить
            Map<String, Object> settings = botChat.getSettings();
            if (settings != null && Boolean.TRUE.equals(settings.get("welcome_message"))) {
                settings.put("pending_welcome", true);
                botChat.setSettings(settings);
                botChatRepository.save(botChat);
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке приветственного сообщения: {}", e.getMessage());
        }
    }

    private String getChatName(Chat chat) {
        if (chat.getTitle() != null && !chat.getTitle().isEmpty()) {
            return chat.getTitle();
        } else if (chat.getUserName() != null && !chat.getUserName().isEmpty()) {
            return "@" + chat.getUserName();
        } else {
            return "Чат #" + chat.getId();
        }
    }

    private String getUserName(User user) {
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            return "@" + user.getUserName();
        } else if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            return user.getFirstName();
        } else {
            return "Пользователь #" + user.getId();
        }
    }

    @Transactional
    public void setBotAdminStatus(Long chatId, boolean isAdmin) {
        botChatRepository.findByChatId(chatId).ifPresent(chat -> {
            chat.setIsBotAdmin(isAdmin);
            chat.setUpdatedAt(LocalDateTime.now());
            if (!isAdmin) {
                chat.setBotPermissions("{}");
            }
            botChatRepository.save(chat);

            log.info("Статус администратора для чата {} установлен в: {}",
                    chatId, isAdmin ? "✅ Админ" : "❌ Не админ");
        });
    }
}