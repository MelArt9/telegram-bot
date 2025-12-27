package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.melnikov.telegrambot.dto.UserDto;
import ru.melnikov.telegrambot.model.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto toDto(User entity);

    @Mapping(target = "groups", ignore = true)
    User toEntity(UserDto dto);
}
