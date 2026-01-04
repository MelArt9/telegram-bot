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

    @Query("SELECT c FROM BotChat c WHERE c.isActive = true AND c.chatType IN ('GROUP', 'SUPERGROUP')")
    List<BotChat> findAllActiveGroups();

    boolean existsByChatId(Long chatId);

    // Упрощаем запросы для JSON полей
    @Query("SELECT c FROM BotChat c WHERE c.isActive = true")
    List<BotChat> findAllActiveChats();

    // Для работы с JSON в PostgreSQL используем функцию jsonb_extract_path_text
    @Query(value = "SELECT * FROM bot_chats c WHERE c.is_active = true AND " +
            "jsonb_extract_path_text(c.settings, 'schedule_notifications') = 'true'",
            nativeQuery = true)
    List<BotChat> findChatsWithScheduleNotifications();

    @Query(value = "SELECT * FROM bot_chats c WHERE c.is_active = true AND " +
            "jsonb_extract_path_text(c.settings, 'deadline_notifications') = 'true'",
            nativeQuery = true)
    List<BotChat> findChatsWithDeadlineNotifications();
}