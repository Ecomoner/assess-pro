package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.TesterStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsExportServiceTest {

    @Mock
    private TesterStatisticsService testerStatisticsService;

    @InjectMocks
    private StatisticsExportService statisticsExportService;

    private Test test;
    private User creator;
    private Category category;
    private TesterAttemptDTO attemptDTO;
    private TesterDetailedAnswersDTO detailedAnswers;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        category = new Category();
        category.setId(1L);
        category.setName("Математика");

        test = new Test();
        test.setId(1L);
        test.setTitle("Тест по математике");
        test.setDescription("Описание");
        test.setCreatedBy(creator);
        test.setCategory(category);
        test.setTimeLimitMinutes(30);
        test.setRetryCooldownHours(24);
        test.setRetryCooldownDays(1);

        attemptDTO = new TesterAttemptDTO();
        attemptDTO.setAttemptId(1L);
        attemptDTO.setTesterUsername("tester");
        attemptDTO.setTesterFullName("Петров Иван");
        attemptDTO.setStartTime(LocalDateTime.now().minusHours(1));
        attemptDTO.setEndTime(LocalDateTime.now());
        attemptDTO.setScore(8);
        attemptDTO.setMaxScore(10);
        attemptDTO.setPercentage(80.0);
        attemptDTO.setDurationMinutes(60L);
        attemptDTO.setEndTime(LocalDateTime.now());
        assertThat(attemptDTO.getStatus()).isEqualTo("Успешно");

        detailedAnswers = new TesterDetailedAnswersDTO();
        detailedAnswers.setAttemptId(1L);
        detailedAnswers.setTesterUsername("tester");
        // ... заполнить
    }

    @org.junit.jupiter.api.Test
    @DisplayName("generateTestStatisticsPDF: с конкретным тестировщиком")
    void generateTestStatisticsPDF_WithTester_Success() {
        Page<TesterAttemptDTO> page = new PageImpl<>(List.of(attemptDTO));
        when(testerStatisticsService.getTestersByTest(eq(1L), isNull(), any(PageRequest.class)))
                .thenReturn(page);
        when(testerStatisticsService.getTesterDetailedAnswers(eq(1L), isNull()))
                .thenReturn(detailedAnswers);

        byte[] pdfBytes = statisticsExportService.generateTestStatisticsPDF(test, "tester", 1L);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("generateTestStatisticsPDF: без тестировщика (все)")
    void generateTestStatisticsPDF_AllTesters_Success() {
        Page<TesterAttemptDTO> page = new PageImpl<>(List.of(attemptDTO));
        when(testerStatisticsService.getTestersByTest(eq(1L), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        byte[] pdfBytes = statisticsExportService.generateTestStatisticsPDF(test, null, 1L);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}