package ru.melnikov.telegrambot.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.melnikov.telegrambot.model.CommandLog;
import ru.melnikov.telegrambot.repository.CommandLogRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandLogService {

    private final CommandLogRepository repository;

    /**
     * Логирование успешной команды
     */
    @Transactional
    public CommandLog logSuccess(Long userId, String username, Long chatId,
                                 String command, String args, Long executionTimeMs) {
        CommandLog commandLog = CommandLog.builder()
                .userId(userId)
                .username(username)
                .chatId(chatId)
                .command(command)
                .arguments(args)
                .success(true)
                .executionTimeMs(executionTimeMs)
                .createdAt(LocalDateTime.now())
                .build();

        CommandLog saved = repository.save(commandLog);
        log.debug("✅ Команда успешно залогирована: {} от пользователя {}", command, username);

        return saved;
    }

    /**
     * Логирование команды с ошибкой
     */
    @Transactional
    public CommandLog logError(Long userId, String username, Long chatId,
                               String command, String args, String errorMessage, Long executionTimeMs) {
        CommandLog commandLog = CommandLog.builder()
                .userId(userId)
                .username(username)
                .chatId(chatId)
                .command(command)
                .arguments(args)
                .success(false)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .createdAt(LocalDateTime.now())
                .build();

        CommandLog saved = repository.save(commandLog);
        log.error("❌ Ошибка команды залогирована: {} от пользователя {}. Ошибка: {}",
                command, username, errorMessage);

        return saved;
    }

    /**
     * Быстрый лог (без измерения времени)
     */
    @Transactional
    public void quickLog(Long userId, String username, Long chatId, String command) {
        CommandLog commandLog = CommandLog.builder()
                .userId(userId)
                .username(username)
                .chatId(chatId)
                .command(command)
                .success(true)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(commandLog);
        log.info("📝 Быстрый лог: {} от {}", command, username);
    }

    /**
     * Статистика по командам
     */
    @Transactional(readOnly = true)
    public CommandStatistics getStatistics(LocalDateTime from, LocalDateTime to) {
        List<Object[]> stats = repository.getCommandStatistics(from, to);

        CommandStatistics result = new CommandStatistics();
        for (Object[] row : stats) {
            String cmd = (String) row[0];
            Long count = (Long) row[1];
            Long avgTime = (Long) row[2];
            Long errorCount = (Long) row[3];

            // Исправлено: создаем CommandStat и добавляем в map
            CommandStatistics.CommandStat stat = CommandStatistics.CommandStat.builder()
                    .count(count != null ? count : 0)
                    .avgExecutionTime(avgTime != null ? avgTime : 0)
                    .errorCount(errorCount != null ? errorCount : 0)
                    .build();

            result.getCommandStats().put(cmd, stat);
        }

        // Рассчитываем общие метрики
        result.calculateTotalMetrics();

        return result;
    }

    /**
     * Получение логов пользователя
     */
    @Transactional(readOnly = true)
    public List<CommandLog> getUserLogs(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Получение последних логов
     */
    @Transactional(readOnly = true)
    public List<CommandLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findTopNByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Очистка старых логов
     */
    @Transactional
    public int cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        return repository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * Вспомогательный класс для статистики
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandStatistics {
        private Map<String, CommandStat> commandStats = new HashMap<>();
        private long totalCommands;
        private long totalErrors;
        private double avgExecutionTime;

        /**
         * Рассчитать общие метрики на основе статистики команд
         */
        public void calculateTotalMetrics() {
            totalCommands = 0;
            totalErrors = 0;
            long totalExecutionTime = 0;
            int commandsWithTime = 0;

            for (CommandStat stat : commandStats.values()) {
                totalCommands += stat.getCount();
                totalErrors += stat.getErrorCount();

                if (stat.getAvgExecutionTime() > 0 && stat.getCount() > 0) {
                    totalExecutionTime += stat.getAvgExecutionTime() * stat.getCount();
                    commandsWithTime += stat.getCount();
                }
            }

            avgExecutionTime = commandsWithTime > 0 ?
                    (double) totalExecutionTime / commandsWithTime : 0;
        }

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CommandStat {
            private long count;
            private long avgExecutionTime;
            private long errorCount;

            public double getErrorRate() {
                return count > 0 ? (double) errorCount / count * 100 : 0;
            }

            public double getAvgExecutionTimeMs() {
                return avgExecutionTime;
            }
        }
    }
}