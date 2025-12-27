package ru.melnikov.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.GroupDto;
import ru.melnikov.telegrambot.mapper.GroupMapper;
import ru.melnikov.telegrambot.service.GroupService;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GroupController {

    private final GroupService groupService;
    private final GroupMapper groupMapper;

    @GetMapping
    public List<GroupDto> getAll() {
        return groupService.findAll().stream()
                .map(groupMapper::toDto)
                .toList();
    }

    @PostMapping
    public GroupDto create(@Valid @RequestBody GroupDto groupDto) {
        return groupMapper.toDto(groupService.save(groupMapper.toEntity(groupDto)));
    }
}
