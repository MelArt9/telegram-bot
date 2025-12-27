package ru.melnikov.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    public List<LinkDto> getAll() {
        return linkService.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<LinkDto> getByUser(@PathVariable Long userId) {
        return linkService.findByUser(userId);
    }

    @PostMapping
    public LinkDto create(@Valid @RequestBody LinkDto link) {
        return linkService.save(link);
    }
}
