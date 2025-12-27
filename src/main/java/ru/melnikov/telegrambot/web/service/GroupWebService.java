package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.service.GroupService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupWebService {

    private final GroupService groupService;

    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    public Group getById(Long id) {
        return groupService.findById(id);
    }
}
