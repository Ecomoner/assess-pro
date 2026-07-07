package com.frist.assesspro.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterProjectStatsDTO {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private Long attemptCount;
    private Double averageScore;
    private LocalDateTime lastAttemptDate;

    public String getFullName(){
        StringBuilder sb = new StringBuilder();
        if(lastName != null) sb.append(lastName);
        if(firstName != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(firstName);
        }
        return sb.isEmpty() ? username : sb.toString();
    }
}
