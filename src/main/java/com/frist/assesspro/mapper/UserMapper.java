package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    UserManagementDTO toDto(User user);

    List<UserManagementDTO> toDtoList(List<User> users);
}
