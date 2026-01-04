package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.melnikov.telegrambot.model.Reminder;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByChatIdAndIsActiveTrue(Long chatId);

    @Query("SELECT r FROM Reminder r WHERE r.isActive = true AND r.scheduleTime = :time")
    List<Reminder> findActiveRemindersByTime(@Param("time") LocalTime time);

    @Query("SELECT r FROM Reminder r WHERE r.isActive = true AND r.chatId = :chatId AND r.reminderType = :type")
    Optional<Reminder> findByChatIdAndType(@Param("chatId") Long chatId,
                                           @Param("type") String type);

    @Query("SELECT r FROM Reminder r WHERE r.chatId = :chatId AND r.createdBy.id = :userId")
    List<Reminder> findByChatIdAndCreatedBy(@Param("chatId") Long chatId,
                                            @Param("userId") Long userId);

    @Query("SELECT r FROM Reminder r WHERE r.isActive = true AND r.reminderType = :type")
    List<Reminder> findAllByType(@Param("type") String type);
}