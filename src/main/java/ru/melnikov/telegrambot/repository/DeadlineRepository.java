package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.melnikov.telegrambot.model.Deadline;

import java.time.LocalDateTime;
import java.util.List;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    List<Deadline> findByDeadlineAtAfter(LocalDateTime dateTime);

    List<Deadline> findByCreatedBy_Id(Long userId);
}