package com.frist.assesspro.pdf;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.ManagerService;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerPdfExportService {

    private final PdfFontProvider fontProvider;
    private final ManagerService managerService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int MAX_EXPORT_SIZE = 1000;

    /**
     * Генерация статистики теста для менеджера (только тестировщики его проектов).
     */
    public byte[] generateTestStatistics(Test test, String managerUsername, String testerUsername, Long categoryId) {
        log.info("Генерация PDF статистики теста {} для менеджера {}", test.getId(), managerUsername);

        // Получаем ВСЕ попытки (без пагинации) с фильтрацией по проектам
        List<TesterAttemptDTO> allAttempts = managerService.getAllFilteredTestersByTest(test.getId(), managerUsername);
        if (allAttempts.isEmpty()) {
            return generateEmptyReport(test, "Нет данных для отчёта");
        }

        // Фильтруем по конкретному тестировщику, если указан
        List<TesterAttemptDTO> filtered = allAttempts;
        if (testerUsername != null && !testerUsername.isEmpty()) {
            filtered = allAttempts.stream()
                    .filter(a -> a.getTesterUsername().equals(testerUsername))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                return generateEmptyReport(test, "Тестировщик " + testerUsername + " не найден или не проходил тест");
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            var font = fontProvider.getFont();
            document.setFont(font);

            // Заголовок
            document.add(new Paragraph("Статистика по тесту")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(""));

            // Информация о тесте
            document.add(createTestInfoTable(test, font));
            document.add(new Paragraph(""));

            if (testerUsername != null && !testerUsername.isEmpty()) {
                addSingleTesterStatistics(document, test.getId(), testerUsername, managerUsername, font);
            } else {
                addAllTestersStatistics(document, test.getId(), managerUsername, font);
            }

            PdfCommonUtils.addFooter(document, font);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Ошибка при генерации PDF для менеджера", e);
            throw new RuntimeException("Ошибка генерации PDF: " + e.getMessage());
        }
    }

    // ------------------ Вспомогательные методы ------------------

    private Table createTestInfoTable(Test test, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addCell(PdfCommonUtils.createCell("Название теста:", true, font));
        table.addCell(PdfCommonUtils.createCell(test.getTitle(), false, font));

        table.addCell(PdfCommonUtils.createCell("Описание:", true, font));
        table.addCell(PdfCommonUtils.createCell(test.getDescription() != null ? test.getDescription() : "-", false, font));

        table.addCell(PdfCommonUtils.createCell("Категория:", true, font));
        table.addCell(PdfCommonUtils.createCell(test.getCategory() != null ? test.getCategory().getName() : "Без категории", false, font));

        table.addCell(PdfCommonUtils.createCell("Количество вопросов:", true, font));
        table.addCell(PdfCommonUtils.createCell(String.valueOf(test.getQuestionCount()), false, font));

        return table;
    }

    private void addSingleTesterStatistics(Document document, Long testId, String testerUsername,
                                           String managerUsername, PdfFont font) {

        List<TesterAttemptDTO> allAttempts = managerService.getAllFilteredTestersByTest(testId, managerUsername);
        List<TesterAttemptDTO> attempts = allAttempts.stream()
                .filter(a -> a.getTesterUsername().equals(testerUsername))
                .collect(Collectors.toList());

        if (attempts.isEmpty()) {
            document.add(new Paragraph("Нет данных по тестировщику " + testerUsername).setFont(font));
            return;
        }

        TesterAttemptDTO lastAttempt = attempts.get(0);
        document.add(new Paragraph("Статистика тестировщика: " + testerUsername)
                .setFontSize(16).setBold().setFont(font));
        document.add(new Paragraph(""));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));
        infoTable.addCell(PdfCommonUtils.createCell("Всего попыток:", true, font));
        infoTable.addCell(PdfCommonUtils.createCell(String.valueOf(attempts.size()), false, font));
        infoTable.addCell(PdfCommonUtils.createCell("Последняя попытка:", true, font));
        infoTable.addCell(PdfCommonUtils.createCell(lastAttempt.getStartTime() != null ?
                lastAttempt.getStartTime().format(DATE_FORMATTER) : "-", false, font));
        infoTable.addCell(PdfCommonUtils.createCell("Результат:", true, font));
        infoTable.addCell(PdfCommonUtils.createCell(String.format("%d/%d (%.1f%%)",
                lastAttempt.getScore(), lastAttempt.getMaxScore(),
                lastAttempt.getPercentage() != null ? lastAttempt.getPercentage() : 0.0), false, font));
        infoTable.addCell(PdfCommonUtils.createCell("Длительность:", true, font));
        infoTable.addCell(PdfCommonUtils.createCell(lastAttempt.getFormattedDuration(), false, font));
        document.add(infoTable);
        document.add(new Paragraph(""));

        // Детальные ответы (можно добавить через getFilteredTesterDetailedAnswers)

        document.add(new Paragraph("Детальные ответы доступны в веб-интерфейсе").setFont(font));
    }

    private void addAllTestersStatistics(Document document, Long testId, String managerUsername, PdfFont font) {
        List<TesterAttemptDTO> allAttempts = managerService.getAllFilteredTestersByTest(testId, managerUsername);
        if (allAttempts.isEmpty()) {
            document.add(new Paragraph("Нет попыток для этого теста").setFont(font));
            return;
        }

        Map<String, List<TesterAttemptDTO>> attemptsByUser = allAttempts.stream()
                .collect(Collectors.groupingBy(TesterAttemptDTO::getTesterUsername));

        document.add(new Paragraph("Список тестировщиков")
                .setFontSize(16).setBold().setFont(font));
        document.add(new Paragraph(""));

        for (Map.Entry<String, List<TesterAttemptDTO>> entry : attemptsByUser.entrySet()) {
            String username = entry.getKey();
            List<TesterAttemptDTO> userAttempts = entry.getValue();

            double bestPercentage = userAttempts.stream()
                    .mapToDouble(a -> a.getPercentage() != null ? a.getPercentage() : 0.0)
                    .max().orElse(0.0);
            TesterAttemptDTO lastAttempt = userAttempts.stream()
                    .max(Comparator.comparing(TesterAttemptDTO::getStartTime))
                    .orElse(null);

            document.add(new Paragraph("Тестировщик: " + username)
                    .setFontSize(14).setBold().setFont(font));

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));
            summaryTable.addCell(PdfCommonUtils.createCell("Всего попыток:", true, font));
            summaryTable.addCell(PdfCommonUtils.createCell(String.valueOf(userAttempts.size()), false, font));
            if (lastAttempt != null) {
                String lastDate = lastAttempt.getStartTime() != null ?
                        lastAttempt.getStartTime().format(DATE_FORMATTER) : "-";
                summaryTable.addCell(PdfCommonUtils.createCell("Последняя попытка:", true, font));
                summaryTable.addCell(PdfCommonUtils.createCell(lastDate, false, font));
                summaryTable.addCell(PdfCommonUtils.createCell("Результат последней:", true, font));
                summaryTable.addCell(PdfCommonUtils.createCell(String.format("%d/%d (%.1f%%)",
                        lastAttempt.getScore(), lastAttempt.getMaxScore(),
                        lastAttempt.getPercentage() != null ? lastAttempt.getPercentage() : 0.0), false, font));
            }
            summaryTable.addCell(PdfCommonUtils.createCell("Лучший результат:", true, font));
            summaryTable.addCell(PdfCommonUtils.createCell(String.format("%.1f%%", bestPercentage), false, font));

            document.add(summaryTable);
            document.add(new Paragraph(""));
        }
    }

    private byte[] generateEmptyReport(Test test, String message) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setFont(fontProvider.getFont());
            document.add(new Paragraph("Отчёт по тесту \"" + test.getTitle() + "\"")
                    .setFontSize(18).setBold());
            document.add(new Paragraph(message));
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания пустого отчёта", e);
        }
    }
}