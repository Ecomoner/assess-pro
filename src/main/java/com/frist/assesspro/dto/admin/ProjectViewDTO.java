package com.frist.assesspro.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectViewDTO {

    private Long id;
    private String name;
    private String description;
    private boolean active;
    private ManagerInfo manager;
    private List<TesterInfo> testers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ManagerInfo {
        private Long id;
        private String username;
        private String fullName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TesterInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}
