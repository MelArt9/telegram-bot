package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.melnikov.telegrambot.dto.GroupDto;
import ru.melnikov.telegrambot.model.Group;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupMapper {

    GroupDto toDto(Group entity);

    @Mapping(target = "users", ignore = true)
    Group toEntity(GroupDto dto);
}
