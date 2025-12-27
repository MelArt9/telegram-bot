package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.model.Link;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LinkMapper {

    @Mapping(source = "createdBy.id", target = "createdBy")
    LinkDto toDto(Link entity);

    @Mapping(target = "createdBy", ignore = true)
    Link toEntity(LinkDto dto);
}
