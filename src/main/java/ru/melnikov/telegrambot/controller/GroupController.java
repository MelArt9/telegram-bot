package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.service.GroupService;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<Group> getAll() {
        return groupService.findAll();
    }

    @PostMapping
    public Group create(@RequestBody Group group) {
        return groupService.save(group);
    }
}