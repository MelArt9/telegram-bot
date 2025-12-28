package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.model.User;

@Mapper(componentModel = "spring")
public interface DeadlineMapper {

    @Mapping(source = "createdBy.id", target = "createdBy")
    DeadlineDto toDto(Deadline entity);

    @Mapping(target = "createdBy", source = "createdBy")
    Deadline toEntity(DeadlineDto dto);

    void updateEntity(DeadlineDto dto, @MappingTarget Deadline entity);

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======

    default Long mapUserToId(User user) {
        return user != null ? user.getId() : null;
    }

    default User mapIdToUser(Long id) {
        if (id == null) return null;
        User u = new User();
        u.setId(id);
        return u;
    }
}