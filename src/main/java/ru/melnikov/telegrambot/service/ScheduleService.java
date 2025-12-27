package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.ScheduleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<Schedule> findByDay(Integer dayOfWeek) {
        return scheduleRepository.findByDayOfWeek(dayOfWeek);
    }

    public List<Schedule> findByDayAndWeekType(Integer day, String weekType) {
        return scheduleRepository.findByDayOfWeekAndWeekType(day, weekType);
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }
}