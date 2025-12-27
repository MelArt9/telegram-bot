package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.repository.GroupRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Optional<Group> findByName(String name) {
        return groupRepository.findByName(name);
    }

    public Group save(Group group) {
        return groupRepository.save(group);
    }
}