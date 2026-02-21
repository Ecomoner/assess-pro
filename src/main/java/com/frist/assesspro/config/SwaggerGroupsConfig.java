package com.frist.assesspro.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerGroupsConfig {

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/admin/**")
                .packagesToScan("com.frist.assesspro.controllers.admin")
                .build();
    }

    @Bean
    public GroupedOpenApi creatorApi() {
        return GroupedOpenApi.builder()
                .group("creator")
                .pathsToMatch("/creator/**")
                .packagesToScan("com.frist.assesspro.controllers.creator")
                .build();
    }

    @Bean
    public GroupedOpenApi testerApi() {
        return GroupedOpenApi.builder()
                .group("tester")
                .pathsToMatch("/tester/**")
                .packagesToScan("com.frist.assesspro.controllers.tester")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/auth/**", "/home", "/login", "/register")
                .packagesToScan("com.frist.assesspro.controllers")
                .build();
    }
}
