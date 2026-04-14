package com.cariesguard.report.infrastructure.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportPdfService {

    private static final float FONT_SIZE = 11F;
    private static final float TITLE_FONT_SIZE = 15F;
    private static final float LEADING = 16F;
    private static final float MARGIN = 48F;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    public byte[] generatePdf(String renderedContent) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = loadChineseCapableFont(document);
            List<String> lines = wrapLines(renderedContent, font, FONT_SIZE, PAGE_WIDTH - (MARGIN * 2));
            writePages(document, font, lines);
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Generate report PDF failed", exception);
        }
    }

    private PDFont loadChineseCapableFont(PDDocument document) throws IOException {
        for (String path : fontCandidates()) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return PDType0Font.load(document, file);
            }
        }
        return PDType1Font.HELVETICA;
    }

    private List<String> fontCandidates() {
        return List.of(
                "C:/Windows/Fonts/NotoSansSC-VF.ttf",
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/simsunb.ttf",
                "C:/Windows/Fonts/SimsunExtG.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
    }

    private void writePages(PDDocument document, PDFont font, List<String> lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = startContent(document, page, font);
        int lineIndex = 0;
        float y = PAGE_HEIGHT - MARGIN - TITLE_FONT_SIZE - 18F;
        try {
            while (lineIndex < lines.size()) {
                if (y < MARGIN) {
                    content.endText();
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = startContent(document, page, font);
                    y = PAGE_HEIGHT - MARGIN - TITLE_FONT_SIZE - 18F;
                }
                String safeLine = encodeSafe(font, lines.get(lineIndex));
                content.showText(safeLine);
                content.newLineAtOffset(0, -LEADING);
                y -= LEADING;
                lineIndex++;
            }
            content.endText();
        } finally {
            content.close();
        }
    }

    private PDPageContentStream startContent(PDDocument document, PDPage page, PDFont font) throws IOException {
        PDPageContentStream content = new PDPageContentStream(document, page);
        content.beginText();
        content.setFont(font, TITLE_FONT_SIZE);
        content.newLineAtOffset(MARGIN, PAGE_HEIGHT - MARGIN);
        content.showText(encodeSafe(font, "CariesGuard 龋齿智能分析报告"));
        content.newLineAtOffset(0, -24F);
        content.setFont(font, FONT_SIZE);
        return content;
    }

    private List<String> wrapLines(String renderedContent, PDFont font, float fontSize, float maxWidth) throws IOException {
        String normalized = StringUtils.hasText(renderedContent) ? renderedContent : "Empty report content";
        String[] rawLines = normalized.split("\\R", -1);
        List<String> result = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                result.add(" ");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < line.length(); ) {
                int codePoint = line.codePointAt(offset);
                String token = new String(Character.toChars(codePoint));
                String candidate = current + token;
                if (!current.isEmpty() && stringWidth(font, encodeSafe(font, candidate), fontSize) > maxWidth) {
                    result.add(current.toString());
                    current.setLength(0);
                    current.append(token);
                } else {
                    current.append(token);
                }
                offset += Character.charCount(codePoint);
            }
            if (!current.isEmpty()) {
                result.add(current.toString());
            }
        }
        return result;
    }

    private float stringWidth(PDFont font, String text, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000F * fontSize;
    }

    private String encodeSafe(PDFont font, String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String token = new String(Character.toChars(codePoint));
            try {
                font.encode(token);
                builder.append(token);
            } catch (IllegalArgumentException exception) {
                builder.append('?');
            } catch (IOException exception) {
                builder.append('?');
            }
            offset += Character.charCount(codePoint);
        }
        return builder.toString();
    }
}
