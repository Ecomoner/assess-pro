package com.frist.assesspro.pdf;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
public class PdfFontProvider {

    private PdfFont font;

    @PostConstruct
    public void init() {
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/arial.ttf")) {
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
                font = PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
                log.info("Шрифт Arial успешно загружен из ресурсов");
            } else {
                log.warn("Файл шрифта /fonts/arial.ttf не найден, используется Helvetica");
                font = PdfFontFactory.createFont("Helvetica", "Cp1251",
                        PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки шрифта, используется Helvetica", e);
            try {
                font = PdfFontFactory.createFont("Helvetica", "Cp1251",
                        PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new RuntimeException("Невозможно загрузить шрифт для PDF", ex);
            }
        }
    }

    public PdfFont getFont() {
        return font;
    }
}
