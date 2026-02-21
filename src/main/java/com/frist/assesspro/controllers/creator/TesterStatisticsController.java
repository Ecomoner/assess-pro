package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CooldownService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mysql.cj.util.TimeUtil.DATE_FORMATTER;

@Controller
@RequestMapping("/creator/tests/{testId}/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Статистика тестера",description = "API для создателей")
public class TesterStatisticsController {

    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;
    private final CooldownService cooldownService;
    private final UserService userService;

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
            Page<TesterStatisticsDTO> testersPage = testerStatisticsService.getTestersStatistics(
                    testId, userDetails.getUsername(), search, pageable);

            model.addAttribute("test", test);
            model.addAttribute("testers", testersPage.getContent());
            model.addAttribute("currentPage", testersPage.getNumber());
            model.addAttribute("totalPages", testersPage.getTotalPages());
            model.addAttribute("totalItems", testersPage.getTotalElements());
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
    public ResponseEntity<byte[]> exportTesterResults(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            TesterDetailedAnswersDTO detailedAnswers = testerStatisticsService
                    .getTesterDetailedAnswers(attemptId, userDetails.getUsername());

            // Получаем полное имя тестировщика
            User tester = userService.findByUsername(detailedAnswers.getTesterUsername()).orElse(null);
            String testerFullName = tester != null ? tester.getFullName() : detailedAnswers.getTesterUsername();

            // Генерация PDF с использованием iText
            byte[] pdfContent = generateTesterAttemptPdf(detailedAnswers, testerFullName);

            String filename = String.format("attempt_%d_%s.pdf",
                    attemptId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("Ошибка при экспорте результатов", e);
            return ResponseEntity.badRequest().build();
        }
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

    /**
     * Генерация PDF для конкретной попытки тестировщика
     */
    private byte[] generateTesterAttemptPdf(TesterDetailedAnswersDTO detailedAnswers, String testerFullName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Загружаем шрифт с поддержкой кириллицы
            PdfFont font = loadFont();
            document.setFont(font);

            // Заголовок
            document.add(new Paragraph("Результаты прохождения теста")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(""));

            // Информация о тестировщике (теперь с полным именем)
            document.add(createTesterInfoTable(detailedAnswers, testerFullName, font));
            document.add(new Paragraph(""));

            // Сводка результатов
            document.add(createSummaryTable(detailedAnswers.getSummary(), font));
            document.add(new Paragraph(""));

            // Детальные ответы
            document.add(createDetailedAnswersTable(detailedAnswers, font));

            // Дата генерации
            document.add(new Paragraph(""));
            document.add(new Paragraph("Отчет сгенерирован: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка при генерации PDF", e);
            throw new RuntimeException("Ошибка генерации PDF", e);
        }
    }

    /**
     * Создание таблицы с информацией о тестировщике
     */
    private Table createTesterInfoTable(TesterDetailedAnswersDTO dto, String testerFullName, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addCell(createCell("Тестировщик:", true, font));
        table.addCell(createCell(testerFullName, false, font));  // ← теперь полное имя

        table.addCell(createCell("Логин:", true, font));
        table.addCell(createCell(dto.getTesterUsername(), false, font));

        table.addCell(createCell("Дата начала:", true, font));
        table.addCell(createCell(dto.getStartTime().format(DATE_FORMATTER), false, font));

        table.addCell(createCell("Дата завершения:", true, font));
        String endTimeStr = dto.getEndTime() != null ?
                dto.getEndTime().format(DATE_FORMATTER) : "Не завершен";
        table.addCell(createCell(endTimeStr, false, font));

        table.addCell(createCell("Длительность:", true, font));
        table.addCell(createCell(dto.getFormattedDuration(), false, font));

        return table;
    }

    /**
     * Создание таблицы со сводкой результатов
     */
    private Table createSummaryTable(TestSummaryDTO summary, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addCell(createCell("Всего вопросов:", true, font));
        table.addCell(createCell(String.valueOf(summary.getTotalQuestions()), false, font));

        table.addCell(createCell("Отвечено:", true, font));
        table.addCell(createCell(String.valueOf(summary.getAnsweredQuestions()), false, font));

        table.addCell(createCell("Правильных ответов:", true, font));
        table.addCell(createCell(String.valueOf(summary.getCorrectAnswers()), false, font));

        table.addCell(createCell("Результат:", true, font));
        String resultText = String.format("%d/%d (%.1f%%)",
                summary.getCorrectAnswers(),
                summary.getTotalQuestions(),
                summary.getPercentage());
        Cell resultCell = createCell(resultText, false, font);
        if (summary.getPercentage() >= 70) {
            resultCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN);
        } else if (summary.getPercentage() >= 50) {
            resultCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.ORANGE);
        } else {
            resultCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED);
        }
        table.addCell(resultCell);

        return table;
    }

    /**
     * Создание таблицы с детальными ответами
     */
    private Table createDetailedAnswersTable(TesterDetailedAnswersDTO dto, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 40, 25, 30}))
                .setWidth(UnitValue.createPercentValue(100));

        // Заголовки
        table.addHeaderCell(createCell("№", true, font));
        table.addHeaderCell(createCell("Вопрос", true, font));
        table.addHeaderCell(createCell("Ответ тестировщика", true, font));
        table.addHeaderCell(createCell("Правильный ответ", true, font));

        int num = 1;
        for (QuestionAnswerDetailDTO answer : dto.getQuestionAnswers()) {
            table.addCell(createCell(String.valueOf(num++), true, font));
            table.addCell(createCell(answer.getQuestionText(), false, font));

            // Ответ тестировщика
            String chosenText = answer.getChosenAnswer() != null ?
                    answer.getChosenAnswer().getAnswerText() : "Не отвечено";
            Cell chosenCell = createCell(chosenText, false, font);
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                chosenCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN);
            } else if (answer.getChosenAnswer() != null) {
                chosenCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED);
            }
            table.addCell(chosenCell);

            // Правильный ответ
            String correctText = answer.getCorrectAnswer() != null ?
                    answer.getCorrectAnswer().getAnswerText() : "Нет правильного ответа";
            table.addCell(createCell(correctText, false, font));
        }

        return table;
    }

    /**
     * Загрузка шрифта с поддержкой кириллицы
     */
    private PdfFont loadFont() {
        // Код загрузки шрифта из StatisticsExportService
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/arial.ttf")) {
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить шрифт из ресурсов", e);
        }

        try {
            return PdfFontFactory.createFont("Helvetica", "Cp1251",
                    PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        } catch (Exception e) {
            log.error("Не удалось загрузить шрифт", e);
            throw new RuntimeException("Не удалось загрузить шрифт для PDF", e);
        }
    }

    /**
     * Создание ячейки таблицы
     */
    private Cell createCell(String text, boolean isHeader, PdfFont font) {
        Cell cell = new Cell();
        Paragraph paragraph = new Paragraph(text != null ? text : "-");
        paragraph.setFont(font);

        if (isHeader) {
            paragraph.setBold();
            cell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        }

        cell.add(paragraph);
        cell.setPadding(5);
        return cell;
    }


}




