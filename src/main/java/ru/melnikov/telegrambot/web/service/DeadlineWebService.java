package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.mapper.DeadlineMapper;
import ru.melnikov.telegrambot.service.DeadlineService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineWebService {

    private final DeadlineService deadlineService;
    private final DeadlineMapper deadlineMapper;

    public List<DeadlineDto> findAll() {
        return deadlineService.findAll();
    }

    public DeadlineDto findById(Long id) {
        return deadlineService.findById(id);
    }

    public void save(DeadlineDto dto) {
        deadlineService.save(dto);
    }

    public void delete(Long id) {
        deadlineService.delete(id);
    }
}