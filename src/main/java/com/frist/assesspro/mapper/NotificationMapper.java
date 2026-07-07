package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.material.MaterialDTO;
import com.frist.assesspro.dto.notification.NotificationDTO;
import com.frist.assesspro.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "type",expression = "java(notification.getType().name())")
    @Mapping(target = "targetUrl",ignore = true)
    NotificationDTO toDto(Notification notification);
}
