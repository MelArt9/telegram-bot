package ru.melnikov.telegrambot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.melnikov.telegrambot.model.CommandLog;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    List<CommandLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    default List<CommandLog> findByUserIdOrderByCreatedAtDesc(Long userId, int limit) {
        return findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    List<CommandLog> findByCommandOrderByCreatedAtDesc(String command, Pageable pageable);

    List<CommandLog> findBySuccessFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT cl FROM CommandLog cl ORDER BY cl.createdAt DESC")
    List<CommandLog> findTopNByOrderByCreatedAtDesc(Pageable pageable);

    default List<CommandLog> findTopNByOrderByCreatedAtDesc(int limit) {
        return findTopNByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Query("SELECT COUNT(cl) FROM CommandLog cl WHERE cl.createdAt BETWEEN :from AND :to")
    Long countByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT cl.command, 
               COUNT(cl) as count, 
               COALESCE(AVG(cl.executionTimeMs), 0) as avgTime,
               SUM(CASE WHEN cl.success = false THEN 1 ELSE 0 END) as errorCount
        FROM CommandLog cl 
        WHERE cl.createdAt BETWEEN :from AND :to
        GROUP BY cl.command
        ORDER BY count DESC
    """)
    List<Object[]> getCommandStatistics(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT cl FROM CommandLog cl WHERE cl.userId = :userId AND cl.createdAt BETWEEN :from AND :to ORDER BY cl.createdAt DESC")
    List<CommandLog> findUserLogsByPeriod(@Param("userId") Long userId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Modifying
    @Query("DELETE FROM CommandLog cl WHERE cl.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT cl FROM CommandLog cl WHERE cl.errorMessage IS NOT NULL ORDER BY cl.createdAt DESC")
    List<CommandLog> findErrors(Pageable pageable);
}