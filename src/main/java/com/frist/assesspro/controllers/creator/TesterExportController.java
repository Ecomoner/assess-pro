package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.export.AsyncPdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/creator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Экспорт статистики", description = "API для экспорта статистики в PDF")
@PreAuthorize("hasRole('CREATOR')")
public class TesterExportController {

    private final TesterStatisticsService testerStatisticsService;
    private final UserService userService;
    private final AsyncPdfExportService asyncPdfExportService;

    @Operation(summary = "Экспорт полной статистики тестировщика в PDF")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/tester/{testerUsername}/full-statistics")
    public ResponseEntity<Map<String, String>> exportTesterFullStatistics(
            @PathVariable String testerUsername,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User tester = userService.getUserByUsername(testerUsername);
            List<TesterAttemptDTO> allAttempts = testerStatisticsService.getAllAttemptsByTester(testerUsername);
            if (allAttempts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Нет попыток для экспорта"));
            }
            String requestId = UUID.randomUUID().toString();
            asyncPdfExportService.generateTesterFullStatistics(tester, allAttempts, requestId);
            return ResponseEntity.ok(Map.of("requestId", requestId, "message", "Полный отчёт по тестировщику готовится..."));
        } catch (Exception e) {
            log.error("Ошибка при запуске экспорта полной статистики", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Внутренняя ошибка: " + e.getMessage()));
        }

}
}