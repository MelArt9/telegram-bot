package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.melnikov.telegrambot.model.CommandLog;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {
}