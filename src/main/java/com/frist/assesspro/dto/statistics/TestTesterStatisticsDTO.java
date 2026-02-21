package com.frist.assesspro.dto.statistics;

import lombok.Data;
import java.util.List;

@Data
public class TestTesterStatisticsDTO {
    private Long testId;
    private String testTitle;
    private List<TesterAttemptDTO> attempts;
    private Long totalAttempts;
    private Double averagePercentage;
    private Double bestPercentage;
}