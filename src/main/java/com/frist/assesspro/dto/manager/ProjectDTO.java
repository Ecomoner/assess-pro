package com.frist.assesspro.dto.manager;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDTO {

    private Long id;
    private String name;
    private String manager;
    private Boolean active;
    private Long testersCount;
}
