package com.frist.assesspro.dto.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportFilterDTO {

    private Long testId;
    private String testerUsername;
    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    private String format = "PDF";
    private Boolean includeCorrectAnswers = true;
    private Boolean includeWrongAnswers = true;
    private Boolean includeStats = true;
}