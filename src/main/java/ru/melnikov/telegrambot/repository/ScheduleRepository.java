package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.melnikov.telegrambot.model.Schedule;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByDayOfWeek(Integer dayOfWeek);

    List<Schedule> findByDayOfWeekAndWeekType(Integer dayOfWeek, String weekType);
}