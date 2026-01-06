package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.melnikov.telegrambot.model.BotChat;

import java.util.List;
import java.util.Optional;

public interface BotChatRepository extends JpaRepository<BotChat, Long> {

    Optional<BotChat> findByChatId(Long chatId);

    List<BotChat> findByIsActiveTrue();

    // Новый метод для поиска чатов, где бот администратор
    @Query("SELECT c FROM BotChat c WHERE c.isActive = true AND c.isBotAdmin = true")
    List<BotChat> findActiveAdminChats();

    boolean existsByChatId(Long chatId);

    @Query(value = """
        SELECT * FROM bot_chats c 
        WHERE c.is_active = true 
        AND c.chat_type IN ('group', 'supergroup', 'GROUP', 'SUPERGROUP')
    """, nativeQuery = true)
    List<BotChat> findAllActiveGroups();

    // Упрощаем запросы для JSON полей
    @Query("SELECT c FROM BotChat c WHERE c.isActive = true")
    List<BotChat> findAllActiveChats();

    @Query(value = """
        SELECT * FROM bot_chats c 
        WHERE c.is_active = true 
        AND c.chat_type IN ('group', 'supergroup', 'GROUP', 'SUPERGROUP')
        AND (
           c.settings->>'schedule_notifications' = 'true' 
           OR c.settings->>'schedule_notifications' IS NULL
     )
    """, nativeQuery = true)
    List<BotChat> findChatsWithScheduleNotifications();

    @Query(value = """
        SELECT * FROM bot_chats c 
        WHERE c.is_active = true 
        AND c.chat_type IN ('group', 'supergroup', 'GROUP', 'SUPERGROUP')
        AND (
         c.settings->>'deadline_notifications' = 'true' 
            OR c.settings->>'deadline_notifications' IS NULL
        )
    """, nativeQuery = true)
    List<BotChat> findChatsWithDeadlineNotifications();

    @Query(value = """
        SELECT c.chat_id, 
               COALESCE(c.settings->>'before_class_enabled', 'true')::boolean as before_class_enabled
        FROM bot_chats c 
        WHERE c.is_active = true 
        AND c.chat_type IN ('group', 'supergroup', 'GROUP', 'SUPERGROUP')
        AND (c.settings->>'schedule_notifications' = 'true' OR c.settings->>'schedule_notifications' IS NULL)
    """, nativeQuery = true)
    List<Object[]> findAllActiveGroupsWithBeforeClass();
}