package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "title", source = "name")
    @Mapping(target = "start", source = "eventDate")
    @Mapping(target = "url", expression = "java(\"/tester/events/\" + event.getId())")
    @Mapping(target = "color", constant = "#111827")
    @Mapping(target = "createdByName", source = "createdByEvent.fullName")
    EventDTO toDto(Event event);
}
