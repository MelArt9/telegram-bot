package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.mapper.DeadlineMapper;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.repository.DeadlineRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;
    private final DeadlineMapper deadlineMapper;
    private final UserService userService;

    public List<DeadlineDto> findAll() {
        return deadlineRepository.findAll()
                .stream()
                .map(deadlineMapper::toDto)
                .toList();
    }

    public List<DeadlineDto> findUpcoming() {
        return deadlineRepository.findByDeadlineAtAfter(LocalDateTime.now())
                .stream()
                .map(deadlineMapper::toDto)
                .toList();
    }

    public DeadlineDto create(DeadlineDto dto) {
        Deadline deadline = deadlineMapper.toEntity(dto);
        if (dto.getCreatedBy() != null) {
            deadline.setCreatedBy(userService.getByIdOrThrow(dto.getCreatedBy()));
        }
        return deadlineMapper.toDto(deadlineRepository.save(deadline));
    }
}
