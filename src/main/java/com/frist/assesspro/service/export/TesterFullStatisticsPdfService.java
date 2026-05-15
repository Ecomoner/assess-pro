package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.User;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TesterFullStatisticsPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public byte[] generate(User tester, List<TesterAttemptDTO> attempts) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = loadFont();
            document.setFont(font);

            // Заголовок
            document.add(new Paragraph("Полная статистика тестировщика")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(""));

            // Информация о тестировщике
            document.add(createTesterInfoTable(tester, font));
            document.add(new Paragraph(""));

            // Общая статистика
            document.add(createOverallStatisticsTable(attempts, font));
            document.add(new Paragraph(""));

            // Детальная статистика по тестам
            document.add(createTestsStatisticsTable(attempts, font));

            // Дата генерации
            document.add(new Paragraph(""));
            document.add(new Paragraph("Отчет сгенерирован: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка при генерации PDF", e);
            throw new RuntimeException("Ошибка генерации PDF", e);
        }
    }

    private Table createTesterInfoTable(User tester, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addCell(createCell("Тестировщик:", true, font));
        table.addCell(createCell(tester.getFullName(), false, font));

        table.addCell(createCell("Логин:", true, font));
        table.addCell(createCell(tester.getUsername(), false, font));

        table.addCell(createCell("Роль:", true, font));
        table.addCell(createCell("Тестировщик", false, font));

        table.addCell(createCell("Дата регистрации:", true, font));
        table.addCell(createCell(tester.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), false, font));

        return table;
    }

    private Table createOverallStatisticsTable(List<TesterAttemptDTO> attempts, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(70));

        long totalAttempts = attempts.size();
        long completedAttempts = attempts.stream().filter(a -> a.getEndTime() != null).count();
        long uniqueTests = attempts.stream().map(TesterAttemptDTO::getTestId).distinct().count();

        double avgPercentage = attempts.stream()
                .filter(a -> a.getEndTime() != null)
                .mapToDouble(TesterAttemptDTO::getPercentage)
                .average()
                .orElse(0.0);

        double bestPercentage = attempts.stream()
                .filter(a -> a.getEndTime() != null)
                .mapToDouble(TesterAttemptDTO::getPercentage)
                .max()
                .orElse(0.0);

        long totalDuration = attempts.stream()
                .filter(a -> a.getEndTime() != null)
                .mapToLong(TesterAttemptDTO::getDurationMinutes)
                .sum();

        table.addCell(createCell("Всего попыток:", true, font));
        table.addCell(createCell(String.valueOf(totalAttempts), false, font));

        table.addCell(createCell("Завершено попыток:", true, font));
        table.addCell(createCell(String.valueOf(completedAttempts), false, font));

        table.addCell(createCell("Уникальных тестов:", true, font));
        table.addCell(createCell(String.valueOf(uniqueTests), false, font));

        table.addCell(createCell("Средний результат:", true, font));
        table.addCell(createCell(String.format("%.1f%%", avgPercentage), false, font));

        table.addCell(createCell("Лучший результат:", true, font));
        table.addCell(createCell(String.format("%.1f%%", bestPercentage), false, font));

        table.addCell(createCell("Общее время:", true, font));
        table.addCell(createCell(formatDuration(totalDuration), false, font));

        return table;
    }

    private Table createTestsStatisticsTable(List<TesterAttemptDTO> attempts, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 15, 20, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        // Заголовки
        table.addHeaderCell(createCell("Тест", true, font));
        table.addHeaderCell(createCell("Попыток", true, font));
        table.addHeaderCell(createCell("Средний %", true, font));
        table.addHeaderCell(createCell("Лучший %", true, font));

        // Группируем по тестам
        Map<Long, List<TesterAttemptDTO>> byTest = attempts.stream()
                .collect(Collectors.groupingBy(TesterAttemptDTO::getTestId));

        for (Map.Entry<Long, List<TesterAttemptDTO>> entry : byTest.entrySet()) {
            List<TesterAttemptDTO> testAttempts = entry.getValue();
            String testTitle = testAttempts.get(0).getTestTitle();

            long count = testAttempts.size();
            double avg = testAttempts.stream()
                    .filter(a -> a.getEndTime() != null)
                    .mapToDouble(TesterAttemptDTO::getPercentage)
                    .average()
                    .orElse(0.0);
            double best = testAttempts.stream()
                    .filter(a -> a.getEndTime() != null)
                    .mapToDouble(TesterAttemptDTO::getPercentage)
                    .max()
                    .orElse(0.0);

            table.addCell(createCell(testTitle, false, font));
            table.addCell(createCell(String.valueOf(count), false, font));
            table.addCell(createCell(String.format("%.1f%%", avg), false, font));
            table.addCell(createCell(String.format("%.1f%%", best), false, font));
        }

        return table;
    }

    private String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " мин";
        }
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + " ч " + mins + " мин";
    }

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

    private PdfFont loadFont() {
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/arial.ttf")) {
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить шрифт из ресурсов, используется шрифт по умолчанию", e);
        }

        try {
            return PdfFontFactory.createFont("Helvetica", "Cp1251",
                    PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        } catch (Exception e) {
            log.error("Не удалось загрузить шрифт", e);
            throw new RuntimeException("Не удалось загрузить шрифт для PDF", e);
        }
    }

}
