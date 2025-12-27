package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.model.Deadline;

@Mapper(componentModel = "spring")
public interface DeadlineMapper {

    @Mapping(source = "createdBy.id", target = "createdBy")
    DeadlineDto toDto(Deadline entity);

    @Mapping(target = "createdBy", ignore = true)
    Deadline toEntity(DeadlineDto dto);
}