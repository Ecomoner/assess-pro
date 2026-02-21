package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsExportService {

    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Путь к шрифту в ресурсах
    private static final String FONT_PATH = "/fonts/arial.ttf";

    // Запасной шрифт (встроенный в PDF)
    private static final String FALLBACK_FONT = "Helvetica";

    // Максимальное количество записей для экспорта
    private static final int MAX_EXPORT_SIZE = 1000;

    // Размер страницы для пагинации
    private static final int PAGE_SIZE = 100;

    /**
     * Загрузка шрифта с поддержкой кириллицы
     */
    private PdfFont loadFont() {
        // Пробуем загрузить шрифт из ресурсов
        try (InputStream fontStream = getClass().getResourceAsStream(FONT_PATH)) {
            if (fontStream != null) {
                log.info("Загружен шрифт из ресурсов: {}", FONT_PATH);
                byte[] fontBytes = fontStream.readAllBytes();
                FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
                // Для шрифтов из ресурсов используем IDENTITY_H (Unicode)
                return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
            } else {
                log.warn("Шрифт не найден в ресурсах: {}", FONT_PATH);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить шрифт из ресурсов: {}", e.getMessage());
        }

        // Запасной вариант - использовать встроенный шрифт с поддержкой Unicode
        try {
            log.info("Используем встроенный шрифт Helvetica");
            // Создаем шрифт без указания кодировки - будет использовать стандартную
            return PdfFontFactory.createFont();
        } catch (Exception e) {
            log.error("Не удалось загрузить даже стандартный шрифт", e);
            throw new RuntimeException("Не удалось загрузить шрифт для PDF", e);
        }
    }

    /**
     * Генерация PDF отчета по тесту
     */
    public byte[] generateTestStatisticsPDF(Test test,
                                            String testerUsername,
                                            Long categoryId) {

        log.info("Генерация PDF отчета для теста ID: {}, тестировщик: {}, категория: {}",
                test.getId(), testerUsername, categoryId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Загружаем шрифт
            PdfFont font = loadFont();
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

            // Если указан конкретный тестировщик
            if (testerUsername != null && !testerUsername.isEmpty()) {
                addTesterStatistics(document, test.getId(), testerUsername, font, categoryId);
            } else {
                // Список всех тестировщиков
                addTesterStatistics(document, test.getId(), testerUsername, font, categoryId);
            }

            // Дата генерации
            document.add(new Paragraph(""));
            document.add(new Paragraph("Отчет сгенерирован: " +
                    LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();

            log.info("PDF отчет успешно сгенерирован, размер: {} байт", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка при генерации PDF", e);
            throw new RuntimeException("Ошибка генерации PDF: " + e.getMessage());
        }
    }

    /**
     * Создание таблицы с информацией о тесте
     */
    private Table createTestInfoTable(Test test, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addCell(createCell("Название теста:", true, font));
        table.addCell(createCell(test.getTitle(), false, font));

        table.addCell(createCell("Описание:", true, font));
        table.addCell(createCell(test.getDescription() != null ? test.getDescription() : "-", false, font));

        table.addCell(createCell("Категория:", true, font));
        table.addCell(createCell(test.getCategory() != null ? test.getCategory().getName() : "Без категории", false, font));

        table.addCell(createCell("Количество вопросов:", true, font));
        table.addCell(createCell(String.valueOf(test.getQuestionCount()), false, font));

        table.addCell(createCell("Ограничение на повтор:", true, font));
        table.addCell(createCell(test.getRetryCooldownDisplay(), false, font));

        return table;
    }

    /**
     * Добавление статистики конкретного тестировщика с фильтрацией по категории
     */
    private void addTesterStatistics(Document document, Long testId,
                                     String testerUsername,
                                     PdfFont font, Long categoryId) {  // ← убрали creatorUsername

        try {
            // Получаем все попытки тестировщика с пагинацией
            List<TesterAttemptDTO> allAttempts = new ArrayList<>();
            int page = 0;
            Page<TesterAttemptDTO> attemptsPage;

            do {
                // 🔥 ИСПРАВЛЕНО: Передаем null вместо creatorUsername
                attemptsPage = testerStatisticsService.getTestersByTest(
                        testId, null, PageRequest.of(page++, PAGE_SIZE));
                allAttempts.addAll(attemptsPage.getContent());
            } while (attemptsPage.hasNext() && allAttempts.size() < MAX_EXPORT_SIZE);

            // Фильтруем по тестировщику
            List<TesterAttemptDTO> attempts = allAttempts.stream()
                    .filter(a -> a.getTesterUsername().equals(testerUsername))
                    .collect(Collectors.toList());

            if (attempts.isEmpty()) {
                document.add(new Paragraph("Тестировщик " + testerUsername +
                        " не найден или не проходил этот тест").setFont(font));
                return;
            }

            TesterAttemptDTO lastAttempt = attempts.get(0);

            // Заголовок
            document.add(new Paragraph("Статистика тестировщика: " + testerUsername)
                    .setFontSize(16)
                    .setBold()
                    .setFont(font));
            document.add(new Paragraph(""));

            // Общая информация
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));

            infoTable.addCell(createCell("Всего попыток:", true, font));
            infoTable.addCell(createCell(String.valueOf(attempts.size()), false, font));

            String lastAttemptDate = lastAttempt.getStartTime() != null ?
                    lastAttempt.getStartTime().format(DATE_FORMATTER) : "-";
            infoTable.addCell(createCell("Последняя попытка:", true, font));
            infoTable.addCell(createCell(lastAttemptDate, false, font));

            infoTable.addCell(createCell("Результат:", true, font));
            infoTable.addCell(createCell(String.format("%d/%d (%.1f%%)",
                    lastAttempt.getScore(), lastAttempt.getMaxScore(),
                    lastAttempt.getPercentage() != null ? lastAttempt.getPercentage() : 0.0), false, font));

            infoTable.addCell(createCell("Длительность:", true, font));
            infoTable.addCell(createCell(lastAttempt.getFormattedDuration(), false, font));

            document.add(infoTable);
            document.add(new Paragraph(""));

            // Получаем детальные ответы
            if (!attempts.isEmpty()) {
                // 🔥 ИСПРАВЛЕНО: Передаем null вместо creatorUsername
                addDetailedAnswers(document, testId, lastAttempt.getAttemptId(), null, font);
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении статистики тестировщика", e);
            document.add(new Paragraph("Ошибка загрузки детальной статистики").setFont(font));
        }
    }

    /**
     * Добавление детальных ответов
     */
    private void addDetailedAnswers(Document document, Long testId,
                                    Long attemptId, String creatorUsername,
                                    PdfFont font) {

        try {
            TesterDetailedAnswersDTO detailed = testerStatisticsService
                    .getTesterDetailedAnswers(attemptId, creatorUsername);

            document.add(new Paragraph("Детальные ответы:")
                    .setFontSize(14)
                    .setBold()
                    .setFont(font));
            document.add(new Paragraph(""));

            Table answersTable = new Table(UnitValue.createPercentArray(new float[]{5, 40, 25, 30}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Заголовки таблицы
            answersTable.addHeaderCell(createCell("№", true, font));
            answersTable.addHeaderCell(createCell("Вопрос", true, font));
            answersTable.addHeaderCell(createCell("Ответ тестировщика", true, font));
            answersTable.addHeaderCell(createCell("Правильный ответ", true, font));

            int questionNum = 1;
            for (var answer : detailed.getQuestionAnswers()) {
                answersTable.addCell(createCell(String.valueOf(questionNum++), true, font));
                answersTable.addCell(createCell(answer.getQuestionText(), false, font));

                // Ответ тестировщика
                String chosenText = answer.getChosenAnswer() != null ?
                        answer.getChosenAnswer().getAnswerText() : "Не отвечено";
                Cell chosenCell = createCell(chosenText, false, font);
                if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                    chosenCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN);
                } else if (answer.getChosenAnswer() != null) {
                    chosenCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED);
                }
                answersTable.addCell(chosenCell);

                // Правильный ответ
                String correctText = answer.getCorrectAnswer() != null ?
                        answer.getCorrectAnswer().getAnswerText() : "-";
                answersTable.addCell(createCell(correctText, false, font));
            }

            document.add(answersTable);

        } catch (Exception e) {
            log.error("Ошибка при добавлении детальных ответов", e);
            document.add(new Paragraph("Ошибка загрузки детальных ответов").setFont(font));
        }
    }

    /**
     * Добавление статистики всех тестировщиков с фильтрацией по категории
     */
    private void addAllTestersStatistics(Document document, Long testId,
                                         String creatorUsername, PdfFont font,
                                         Long categoryId) {

        try {
            // Загружаем все попытки с пагинацией
            List<TesterAttemptDTO> allTesters = new ArrayList<>();
            int page = 0;
            Page<TesterAttemptDTO> testersPage;

            do {
                testersPage = testerStatisticsService.getTestersByTest(
                        testId, creatorUsername, PageRequest.of(page++, PAGE_SIZE));
                allTesters.addAll(testersPage.getContent());
            } while (testersPage.hasNext() && allTesters.size() < MAX_EXPORT_SIZE);

            if (allTesters.isEmpty()) {
                document.add(new Paragraph("Нет данных о прохождениях теста").setFont(font));
                return;
            }

            document.add(new Paragraph("Список тестировщиков:")
                    .setFontSize(16)
                    .setBold()
                    .setFont(font));
            document.add(new Paragraph(""));

            Table testersTable = new Table(UnitValue.createPercentArray(new float[]{25, 20, 20, 20, 15}))
                    .setWidth(UnitValue.createPercentValue(100));

            testersTable.addHeaderCell(createCell("Тестировщик", true, font));
            testersTable.addHeaderCell(createCell("Дата", true, font));
            testersTable.addHeaderCell(createCell("Результат", true, font));
            testersTable.addHeaderCell(createCell("Длительность", true, font));
            testersTable.addHeaderCell(createCell("Статус", true, font));

            for (TesterAttemptDTO tester : allTesters) {
                testersTable.addCell(createCell(tester.getTesterUsername(), false, font));

                String startTimeStr = tester.getStartTime() != null ?
                        tester.getStartTime().format(DATE_FORMATTER) : "-";
                testersTable.addCell(createCell(startTimeStr, false, font));

                testersTable.addCell(createCell(String.format("%d/%d (%.1f%%)",
                        tester.getScore(), tester.getMaxScore(),
                        tester.getPercentage() != null ? tester.getPercentage() : 0.0), false, font));

                testersTable.addCell(createCell(tester.getFormattedDuration(), false, font));
                testersTable.addCell(createCell(tester.getStatus(), false, font));
            }

            document.add(testersTable);

            // Общая статистика
            long totalAttempts = allTesters.size();
            long completed = allTesters.stream().filter(t -> t.getEndTime() != null).count();
            double avgScore = allTesters.stream()
                    .filter(t -> t.getPercentage() != null)
                    .mapToDouble(TesterAttemptDTO::getPercentage)
                    .average()
                    .orElse(0.0);

            document.add(new Paragraph(""));
            document.add(new Paragraph("Общая статистика:")
                    .setFontSize(14)
                    .setBold()
                    .setFont(font));

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .setWidth(UnitValue.createPercentValue(50));

            statsTable.addCell(createCell("Всего попыток:", true, font));
            statsTable.addCell(createCell(String.valueOf(totalAttempts), false, font));

            statsTable.addCell(createCell("Завершено:", true, font));
            statsTable.addCell(createCell(String.valueOf(completed), false, font));

            statsTable.addCell(createCell("Средний балл:", true, font));
            statsTable.addCell(createCell(String.format("%.1f%%", avgScore), false, font));

            document.add(statsTable);

            if (totalAttempts >= MAX_EXPORT_SIZE) {
                document.add(new Paragraph(""));
                document.add(new Paragraph("Примечание: показано только " + MAX_EXPORT_SIZE +
                        " записей из-за ограничения размера отчета")
                        .setFontSize(8)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED)
                        .setFont(font));
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении списка тестировщиков", e);
            document.add(new Paragraph("Ошибка загрузки списка тестировщиков").setFont(font));
        }
    }

    /**
     * Создание ячейки таблицы с шрифтом
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