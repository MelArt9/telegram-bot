package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.service.GroupService;
import ru.melnikov.telegrambot.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupWebService {

    private final GroupService groupService;
    private final UserService userService;

    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    public Group getById(Long id) {
        return groupService.findById(id);
    }

    public List<User> getAllUsers() {
        return userService.findAll();
    }

    public void addUserToGroup(Long groupId, Long userId) {
        groupService.addUserToGroup(groupId, userId);
    }

    public void removeUserFromGroup(Long groupId, Long userId) {
        groupService.removeUserFromGroup(groupId, userId);
    }

    public Group getByIdWithUsers(Long id) {
        return groupService.findByIdWithUsers(id);
    }

    public Group save(Group group) {
        return groupService.save(group);
    }

    public void delete(Long id) {
        groupService.delete(id);
    }
}