package com.frist.assesspro.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfCommonUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private PdfCommonUtils() {
        // Утилитный класс – не создаётся
    }

    public static Cell createCell(String text, boolean isHeader, PdfFont font) {
        Cell cell = new Cell();
        Paragraph paragraph = new Paragraph(text != null ? text : "-");
        paragraph.setFont(font);

        if (isHeader) {
            paragraph.setBold();
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }

        cell.add(paragraph);
        cell.setPadding(5);
        return cell;
    }

    public static void addFooter(Document document, PdfFont font) {
        document.add(new Paragraph(""));
        document.add(new Paragraph("Отчет сгенерирован: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT));
    }
}
