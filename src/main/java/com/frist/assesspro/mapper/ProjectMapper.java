package com.frist.assesspro.mapper;

import com.frist.assesspro.dto.manager.ProjectDTO;
import com.frist.assesspro.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMapper {

    @Mapping(target = "manager", source = "manager.fullName")
    @Mapping(target = "testersCount", ignore = true)
    ProjectDTO toDto(Project project);

    List<ProjectDTO> toDtoList(List<Project> projects);
}