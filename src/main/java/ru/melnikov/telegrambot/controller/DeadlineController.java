package ru.melnikov.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.service.DeadlineService;

import java.util.List;

@RestController
@RequestMapping("/api/deadlines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeadlineController {

    private final DeadlineService deadlineService;

    @GetMapping
    public List<DeadlineDto> getAll() {
        return deadlineService.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<DeadlineDto> getByUser(@PathVariable Long userId) {
        return deadlineService.findByUser(userId);
    }

    @PostMapping
    public DeadlineDto create(@Valid @RequestBody DeadlineDto dto) {
        return deadlineService.create(dto);
    }
}
