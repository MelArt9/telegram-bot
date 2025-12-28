package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.ScheduleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleWebService {

    private final ScheduleService scheduleService;

    public List<Schedule> getAll() {
        return scheduleService.findAll();
    }

    public Schedule getById(Long id) {
        return scheduleService.findById(id);
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
