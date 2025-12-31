package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.mapper.DeadlineMapper;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.repository.DeadlineRepository;
import ru.melnikov.telegrambot.util.DeadlineFormatter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;
    private final DeadlineMapper deadlineMapper;

    public List<DeadlineDto> findAll() {
        return deadlineRepository.findAll()
                .stream()
                .map(deadlineMapper::toDto)
                .toList();
    }

    public List<Deadline> findUpcoming() {
        List<Deadline> deadlines = deadlineRepository.findByDeadlineAtAfter(LocalDateTime.now());
        deadlines.sort(Comparator.comparing(Deadline::getDeadlineAt));
        return deadlines;
    }

    public List<Deadline> findAllDeadlines() {
        List<Deadline> deadlines = deadlineRepository.findAll();
        deadlines.sort(Comparator.comparing(Deadline::getDeadlineAt));
        return deadlines;
    }

    public List<Deadline> findAllDeadlinesSorted() {
        return deadlineRepository.findAllByOrderByDeadlineAtAsc();
    }

    public DeadlineDto findById(Long id) {
        return deadlineRepository.findById(id)
                .map(deadlineMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Дедлайн не найден"));
    }

    public DeadlineDto save(DeadlineDto dto) {
        Deadline entity;

        if (dto.getId() != null) {
            // обновление
            entity = deadlineRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Дедлайн не найден"));
            deadlineMapper.updateEntity(dto, entity);
        } else {
            // создание
            entity = deadlineMapper.toEntity(dto);
        }

        return deadlineMapper.toDto(deadlineRepository.save(entity));
    }

    public void delete(Long id) {
        deadlineRepository.deleteById(id);
    }

    public String formatDeadlines() {
        var list = findAllDeadlines();
        return DeadlineFormatter.formatDeadlines(list);
    }

    public String formatUpcomingDeadlines() {
        var list = findUpcoming();
        return DeadlineFormatter.formatDeadlines(list);
    }
}