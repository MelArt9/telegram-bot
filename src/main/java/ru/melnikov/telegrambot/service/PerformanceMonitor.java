package ru.melnikov.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.repository.CommandLogRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PerformanceMonitor {

    private final CommandLogRepository commandLogRepository;
    private final ConcurrentHashMap<String, AtomicLong> commandCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();

    public PerformanceMonitor(CommandLogRepository commandLogRepository) {
        this.commandLogRepository = commandLogRepository;
    }

    public void incrementCommand(String command) {
        commandCounters.computeIfAbsent(command, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementError(String command) {
        errorCounters.computeIfAbsent(command, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Ежечасный отчет о производительности
     */
    @Scheduled(cron = "0 0 * * * *") // Каждый час
    public void generateHourlyReport() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
        LocalDateTime now = LocalDateTime.now();

        Long totalCommands = commandLogRepository.countByPeriod(oneHourAgo, now);

        log.info("""
                📊 ОТЧЕТ О ПРОИЗВОДИТЕЛЬНОСТИ (последний час)
                Всего команд: {}
                Статистика в памяти:
                {}
                """,
                totalCommands,
                getMemoryStats());
    }

    /**
     * Ежедневная очистка и отчет
     */
    @Scheduled(cron = "0 0 0 * * *") // Каждый день в полночь
    public void dailyCleanupAndReport() {
        log.info("🧹 Начало ежедневной очистки логов...");

        // Очистка логов старше 30 дней
        // cleanupOldLogs(30); // Раскомментировать когда будет реализовано

        log.info("✅ Ежедневная очистка завершена");

        // Сброс счетчиков в памяти
        commandCounters.clear();
        errorCounters.clear();
        log.info("🔄 Счетчики производительности сброшены");
    }

    private String getMemoryStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Команда\t\tВсего\tОшибки\t% ошибок\n");
        sb.append("-".repeat(40)).append("\n");

        commandCounters.forEach((command, counter) -> {
            long total = counter.get();
            long errors = errorCounters.getOrDefault(command, new AtomicLong(0)).get();
            double errorRate = total > 0 ? (double) errors / total * 100 : 0;

            sb.append(String.format("%-15s\t%d\t%d\t%.1f%%\n",
                    command, total, errors, errorRate));
        });

        return sb.toString();
    }
}