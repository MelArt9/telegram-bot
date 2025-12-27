package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.Link;
import ru.melnikov.telegrambot.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    public List<Link> getAll() {
        return linkService.findAll();
    }

    @PostMapping
    public Link create(@RequestBody Link link) {
        return linkService.save(link);
    }
}