package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CooldownService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.export.AsyncPdfExportService;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;


@Controller
@RequestMapping("/creator/tests/{testId}/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Статистика тестера",description = "API для создателей")
public class TesterStatisticsController {

    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;
    private final UserService userService;
    private final AsyncPdfExportService asyncPdfExportService;


    @Operation(summary = "Список тестировщиков для статистики")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/testers")
    public String getTestersList(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

            TestSummaryDTO testSummary = testerStatisticsService.getTestSummary(testId, userDetails.getUsername());

            Pageable pageable = PageRequest.of(page, size);
            Page<TesterAttemptDTO> attemptsPage = testerStatisticsService.getTestersByTest(
                    testId, userDetails.getUsername(),search, pageable);

            model.addAttribute("test", test);
            model.addAttribute("testers", attemptsPage.getContent());
            model.addAttribute("currentPage", attemptsPage.getNumber());
            model.addAttribute("totalPages", attemptsPage.getTotalPages());
            model.addAttribute("totalItems", attemptsPage.getTotalElements());
            model.addAttribute("search", search);
            model.addAttribute("testSummary", testSummary);

            return "creator/tester-statistics-main";

        } catch (Exception e) {
            log.error("Ошибка при загрузке списка тестировщиков", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/creator/tests";
        }
    }

    @Operation(summary = "Детальная статистика тестировщика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tester/{attemptId}")
    public String getTesterDetailedAnswers(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            TesterDetailedAnswersDTO detailedAnswers = testerStatisticsService
                    .getTesterDetailedAnswers(attemptId, userDetails.getUsername());

            Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

            User tester = userService.findByUsername(detailedAnswers.getTesterUsername()).orElse(null);
            if (tester != null) {
                model.addAttribute("testerFullName", tester.getFullName());
            }

            model.addAttribute("test", test);
            model.addAttribute("detailedAnswers", detailedAnswers);

            return "creator/tester-detailed-answers";

        } catch (Exception e) {
            log.error("Ошибка при загрузке детальных ответов", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/creator/tests/" + testId + "/statistics/testers";
        }
    }

    @Operation(summary = "Экспорт результатов тестировщика в PDF")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tester/{attemptId}/export")
    public ResponseEntity<Map<String, String>> exportTesterResults(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TesterDetailedAnswersDTO detailedAnswers = testerStatisticsService
                .getTesterDetailedAnswers(attemptId, userDetails.getUsername());
        User tester = userService.findByUsername(detailedAnswers.getTesterUsername()).orElse(null);
        String testerFullName = tester != null ? tester.getFullName() : detailedAnswers.getTesterUsername();

        String requestId = UUID.randomUUID().toString();
        asyncPdfExportService.generateTesterAttemptPdf(detailedAnswers, testerFullName, requestId);

        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "message", "Результаты готовятся..."
        ));
    }

    @Operation(summary = "Быстрый просмотр результатов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tester/{attemptId}/quick-view")
    @ResponseBody
    public TesterDetailedAnswersDTO getQuickView(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return testerStatisticsService.getTesterDetailedAnswers(
                attemptId, userDetails.getUsername());
    }

}




