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

    /**
     * Получить список всех групп
     */
    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    /**
     * Получить группу по id
     */
    public Group getById(Long id) {
        return groupService.findById(id);
    }

    /**
     * Создать или обновить группу
     */
    public Group save(Group group) {
        return groupService.save(group);
    }

    /**
     * Удалить группу
     */
    public void delete(Long id) {
        groupService.delete(id);
    }
}