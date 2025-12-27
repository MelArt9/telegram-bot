package ru.melnikov.telegrambot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.model.Schedule;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

    ScheduleDto toDto(Schedule entity);

    Schedule toEntity(ScheduleDto dto);
}
