// Файл: /src/main/java/ru/melnikov/telegrambot/model/BotChat.java
package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "bot_chats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "chat_type", nullable = false)
    private String chatType;

    @Column(name = "title")
    private String title;

    @Column(name = "username")
    private String username;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_bot_admin")
    private Boolean isBotAdmin; // ← НОВОЕ: является ли бот администратором

    @Column(name = "bot_permissions", columnDefinition = "TEXT")
    private String botPermissions; // ← НОВОЕ: права бота в JSON

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isBotAdmin == null) isBotAdmin = false;
        if (botPermissions == null) botPermissions = "{}";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}