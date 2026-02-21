package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.service.AdminService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminExportService {

    private final AdminService adminService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Путь к шрифту в ресурсах
    private static final String FONT_PATH = "/fonts/arial.ttf";

    // Запасной шрифт (встроенный в PDF)
    private static final String FALLBACK_FONT = "Helvetica";

    /**
     * 🔥 ИСПРАВЛЕНО: Загрузка шрифта с поддержкой кириллицы
     */
    private PdfFont loadFont() {
        // Пробуем загрузить шрифт из ресурсов
        try (InputStream fontStream = getClass().getResourceAsStream(FONT_PATH)) {
            if (fontStream != null) {
                log.info("Загружен шрифт из ресурсов: {}", FONT_PATH);
                return PdfFontFactory.createFont(
                        fontStream.readAllBytes(),
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить шрифт из ресурсов: {}", e.getMessage());
        }

        // Если не удалось, пробуем стандартный шрифт с CP1251 (кириллица)
        try {
            log.info("Используем стандартный шрифт с поддержкой CP1251");
            return PdfFontFactory.createFont(
                    FALLBACK_FONT,
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
        } catch (Exception e) {
            log.error("Не удалось загрузить даже стандартный шрифт", e);
            throw new RuntimeException("Не удалось загрузить шрифт для PDF", e);
        }
    }

    /**
     * 🔥 ИСПРАВЛЕНО: Генерация PDF отчета со статистикой приложения
     */
    public byte[] generateAppStatisticsPDF() {
        log.info("Генерация PDF отчета со статистикой приложения");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Загружаем шрифт
            PdfFont font = loadFont();
            document.setFont(font);

            // Заголовок
            document.add(new Paragraph("Статистика приложения AssessPro")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(""));

            // Получаем статистику
            AppStatisticsDTO stats = adminService.getAppStatistics();

            // Общая статистика
            document.add(createGeneralStatsTable(stats, font));
            document.add(new Paragraph(""));

            // Статистика по пользователям
            document.add(createUserStatsTable(stats, font));
            document.add(new Paragraph(""));

            // Статистика по тестам
            document.add(createTestStatsTable(stats, font));
            document.add(new Paragraph(""));

            // Топ создателей
            document.add(createTopCreatorsTable(stats, font));
            document.add(new Paragraph(""));

            // Топ тестировщиков
            document.add(createTopTestersTable(stats, font));

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
            log.error("Ошибка при генерации PDF отчета", e);
            throw new RuntimeException("Ошибка генерации PDF: " + e.getMessage());
        }
    }

    /**
     * 🔥 НОВОЕ: Генерация списка пользователей в PDF
     */
    public byte[] generateUsersListPDF(String role, Boolean active) {
        log.info("Генерация PDF списка пользователей, роль: {}, активные: {}", role, active);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Загружаем шрифт
            PdfFont font = loadFont();
            document.setFont(font);

            // Заголовок
            String title = "Список пользователей";
            if (role != null) {
                title += " - " + getRoleDisplayName(role);
            }
            if (active != null) {
                title += " (" + (active ? "активные" : "неактивные") + ")";
            }

            document.add(new Paragraph(title)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(""));

            // Получаем пользователей
            PageRequest pageable = PageRequest.of(0, 1000); // Получаем до 1000 пользователей
            var usersPage = adminService.getAllUsers(role, null, active, pageable);
            List<UserManagementDTO> users = usersPage.getContent();

            if (users.isEmpty()) {
                document.add(new Paragraph("Нет пользователей, соответствующих критериям")
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER));
            } else {
                // Создаем таблицу
                Table table = new Table(UnitValue.createPercentArray(new float[]{5, 15, 20, 20, 15, 15, 10}))
                        .setWidth(UnitValue.createPercentValue(100));

                // Заголовки
                table.addHeaderCell(createCell("№", true, font));
                table.addHeaderCell(createCell("Логин", true, font));
                table.addHeaderCell(createCell("ФИО", true, font));
                table.addHeaderCell(createCell("Роль", true, font));
                table.addHeaderCell(createCell("Дата рег.", true, font));
                table.addHeaderCell(createCell("Статус", true, font));
                table.addHeaderCell(createCell("Профиль", true, font));

                // Данные
                int rowNum = 1;
                for (UserManagementDTO user : users) {
                    table.addCell(createCell(String.valueOf(rowNum++), false, font));
                    table.addCell(createCell(user.getUsername(), false, font));
                    table.addCell(createCell(user.getFullName(), false, font));
                    table.addCell(createCell(getRoleDisplayName(user.getRole()), false, font));
                    table.addCell(createCell(user.getCreatedAt().format(DATE_ONLY_FORMATTER), false, font));

                    // Статус активности
                    String status = user.getIsActive() ? "Активен" : "Заблокирован";
                    Cell statusCell = createCell(status, false, font);
                    if (user.getIsActive()) {
                        statusCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN);
                    } else {
                        statusCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED);
                    }
                    table.addCell(statusCell);

                    // Статус профиля
                    String profileStatus = user.getIsProfileComplete() ? "Заполнен" : "Не заполнен";
                    Cell profileCell = createCell(profileStatus, false, font);
                    if (!user.getIsProfileComplete()) {
                        profileCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.ORANGE);
                    }
                    table.addCell(profileCell);
                }

                document.add(table);

                // Итог
                document.add(new Paragraph(""));
                document.add(new Paragraph("Всего пользователей: " + usersPage.getTotalElements())
                        .setFontSize(10)
                        .setBold());
            }

            // Дата генерации
            document.add(new Paragraph(""));
            document.add(new Paragraph("Отчет сгенерирован: " +
                    LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();

            log.info("PDF список пользователей успешно сгенерирован, размер: {} байт", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка при генерации PDF списка пользователей", e);
            throw new RuntimeException("Ошибка генерации PDF: " + e.getMessage());
        }
    }

    /**
     * Получение отображаемого имени роли
     */
    private String getRoleDisplayName(String role) {
        if (role == null) return "-";
        switch (role) {
            case "ROLE_ADMIN": return "Администратор";
            case "ROLE_CREATOR": return "Создатель";
            case "ROLE_TESTER": return "Тестировщик";
            default: return role;
        }
    }

    /**
     * Таблица общей статистики
     */
    private Table createGeneralStatsTable(AppStatisticsDTO stats, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(70));

        table.addCell(createCell("Всего пользователей:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalUsers()), false, font));

        table.addCell(createCell("Всего тестов:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalTests()), false, font));

        table.addCell(createCell("Всего прохождений:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalAttempts()), false, font));

        table.addCell(createCell("Средний балл:", true, font));
        table.addCell(createCell(String.format("%.2f", stats.getAverageScore()), false, font));

        return table;
    }

    /**
     * Таблица статистики по пользователям
     */
    private Table createUserStatsTable(AppStatisticsDTO stats, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(70));

        Cell headerCell = createCell("Статистика по пользователям", true, font);
        headerCell.setFontSize(14);
        headerCell.setBold();
        table.addCell(headerCell);
        table.addCell(createCell("", true, font)); // Пустая ячейка для выравнивания

        table.addCell(createCell("Администраторы:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalAdmins()), false, font));

        table.addCell(createCell("Создатели тестов:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalCreators()), false, font));

        table.addCell(createCell("Тестировщики:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalTesters()), false, font));

        table.addCell(createCell("Незаполненные профили:", true, font));
        table.addCell(createCell(String.valueOf(stats.getUsersWithIncompleteProfile()), false, font));

        return table;
    }

    /**
     * Таблица статистики по тестам
     */
    private Table createTestStatsTable(AppStatisticsDTO stats, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(70));

        Cell headerCell = createCell("Статистика по тестам", true, font);
        headerCell.setFontSize(14);
        headerCell.setBold();
        table.addCell(headerCell);
        table.addCell(createCell("", true, font));

        table.addCell(createCell("Опубликовано тестов:", true, font));
        table.addCell(createCell(String.valueOf(stats.getPublishedTests()), false, font));

        table.addCell(createCell("Черновиков:", true, font));
        table.addCell(createCell(String.valueOf(stats.getDraftTests()), false, font));

        table.addCell(createCell("Всего вопросов:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalQuestions()), false, font));

        table.addCell(createCell("Всего категорий:", true, font));
        table.addCell(createCell(String.valueOf(stats.getTotalCategories()), false, font));

        return table;
    }

    /**
     * Таблица топ создателей
     */
    private Table createTopCreatorsTable(AppStatisticsDTO stats, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell headerCell = createCell("Топ создателей тестов", true, font);
        headerCell.setFontSize(14);
        headerCell.setBold();
        table.addHeaderCell(headerCell);

        table.addHeaderCell(createCell("Создатель", true, font));
        table.addHeaderCell(createCell("Тестов создано", true, font));
        table.addHeaderCell(createCell("Дата регистрации", true, font));

        for (UserManagementDTO creator : stats.getTopCreators()) {
            table.addCell(createCell(creator.getFullName(), false, font));
            table.addCell(createCell(String.valueOf(creator.getTestsCreated()), false, font));
            table.addCell(createCell(creator.getCreatedAt().format(DATE_ONLY_FORMATTER), false, font));
        }

        return table;
    }

    /**
     * Таблица топ тестировщиков
     */
    private Table createTopTestersTable(AppStatisticsDTO stats, PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell headerCell = createCell("Топ тестировщиков", true, font);
        headerCell.setFontSize(14);
        headerCell.setBold();
        table.addHeaderCell(headerCell);

        table.addHeaderCell(createCell("Тестировщик", true, font));
        table.addHeaderCell(createCell("Тестов пройдено", true, font));
        table.addHeaderCell(createCell("Средний балл", true, font));

        for (UserManagementDTO tester : stats.getTopTesters()) {
            table.addCell(createCell(tester.getFullName(), false, font));
            table.addCell(createCell(String.valueOf(tester.getTestsPassed()), false, font));
            table.addCell(createCell(String.format("%.2f", tester.getAverageScore()), false, font));
        }

        return table;
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