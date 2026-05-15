package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.statistics.QuestionAnswerDetailDTO;
import com.frist.assesspro.dto.statistics.TestSummaryDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@Slf4j
public class TesterAttemptPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public byte[] generateTesterAttemptPdf(TesterDetailedAnswersDTO detailedAnswers, String testerFullName) {
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

    @GetMapping
    public String redirectToTestersList(@PathVariable Long testId){
        return "redirect:/creator/tests/" + testId + "/statistics/testers";
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
