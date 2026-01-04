package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "command_logs", indexes = {
        @Index(name = "idx_command_logs_user_id", columnList = "userId"),
        @Index(name = "idx_command_logs_command", columnList = "command"),
        @Index(name = "idx_command_logs_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommandLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "chat_id")
    private Long chatId; // НОВОЕ: ID чата

    @Column(name = "command", nullable = false, length = 50)
    private String command;

    @Column(name = "arguments", columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "success")
    private Boolean success; // НОВОЕ: успешно ли выполнена команда

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // НОВОЕ: сообщение об ошибке

    @Column(name = "execution_time_ms")
    private Long executionTimeMs; // НОВОЕ: время выполнения в мс

    @Column(name = "user_agent")
    private String userAgent; // НОВОЕ: информация о клиенте

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
    }
}