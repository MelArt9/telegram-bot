package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.exception.NotFoundException;
import ru.melnikov.telegrambot.mapper.ScheduleMapper;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.repository.ScheduleRepository;
import ru.melnikov.telegrambot.util.DateUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    public List<ScheduleDto> findByDay(Integer dayOfWeek) {
        return scheduleRepository.findByDayOfWeek(dayOfWeek)
                .stream()
                .map(scheduleMapper::toDto)
                .toList();
    }

    public List<ScheduleDto> findByDayAndWeekType(Integer day, String weekType) {
        return scheduleRepository.findByDayOfWeekAndWeekType(day, weekType)
                .stream()
                .map(scheduleMapper::toDto)
                .toList();
    }

    public List<ScheduleDto> findToday() {
        LocalDate today = LocalDate.now();
        String weekType = DateUtils.weekTypeForDate(today);
        return findByDayAndWeekType(today.getDayOfWeek().getValue(), weekType);
    }

    public ScheduleDto save(ScheduleDto scheduleDto) {
        Schedule schedule = scheduleMapper.toEntity(scheduleDto);
        return scheduleMapper.toDto(scheduleRepository.save(schedule));
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    public Schedule findById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Расписание с id=" + id + " не найдено"));
    }
}
