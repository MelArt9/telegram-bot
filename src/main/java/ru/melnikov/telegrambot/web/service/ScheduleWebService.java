package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.mapper.ScheduleMapper;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.ScheduleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleWebService {

    private final ScheduleService scheduleService;
    private final ScheduleMapper scheduleMapper;

    public List<Schedule> getAll() {
        // Используем метод, который возвращает сущности
        return scheduleService.findAllEntities();
    }

    public Schedule getById(Long id) {
        // Используем метод, который возвращает сущность
        return scheduleService.findEntityById(id);
    }

    public void save(ScheduleDto dto) {
        if (dto.getId() == null) {
            scheduleService.save(dto); // CREATE
        } else {
            scheduleService.update(dto); // UPDATE
        }
    }

    public void delete(Long id) {
        scheduleService.delete(id);
    }
}