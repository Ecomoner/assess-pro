package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterTestStatDTO {
    private Long testId;
    private String testTitle;
    private int totalAttempts;
    private double averageScore;
    private List<TesterAttemptDTO> attempts;
}