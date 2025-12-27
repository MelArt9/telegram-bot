package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.CommandLog;
import ru.melnikov.telegrambot.repository.CommandLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommandLogService {

    private final CommandLogRepository repository;

    public void log(Long userId, String username, String command, String args) {
        CommandLog log = CommandLog.builder()
                .userId(userId)
                .username(username)
                .command(command)
                .arguments(args)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(log);
    }
}