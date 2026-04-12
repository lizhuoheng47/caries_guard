package com.cariesguard.report.infrastructure.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportPdfService {

    public byte[] generatePdf(String renderedContent) {
        String contentStream = buildContentStream(renderedContent);
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + contentStream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n"
                        + contentStream + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            write(output, (i + 1) + " 0 obj\n");
            write(output, objects.get(i));
            write(output, "\nendobj\n");
        }

        int xrefOffset = output.size();
        int totalObjects = objects.size() + 1;
        write(output, "xref\n");
        write(output, "0 " + totalObjects + "\n");
        write(output, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(output, String.format("%010d 00000 n \n", offset));
        }
        write(output, "trailer\n");
        write(output, "<< /Size " + totalObjects + " /Root 1 0 R >>\n");
        write(output, "startxref\n");
        write(output, String.valueOf(xrefOffset));
        write(output, "\n%%EOF");
        return output.toByteArray();
    }

    private String buildContentStream(String renderedContent) {
        List<String> lines = splitLines(renderedContent);
        StringBuilder builder = new StringBuilder();
        builder.append("BT\n");
        builder.append("/F1 12 Tf\n");
        builder.append("50 800 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append("0 -18 Td\n");
            }
            builder.append("(").append(escapePdfText(lines.get(i))).append(") Tj\n");
        }
        builder.append("ET");
        return builder.toString();
    }

    private List<String> splitLines(String renderedContent) {
        String[] rawLines = renderedContent == null ? new String[0] : renderedContent.split("\\R");
        List<String> lines = new ArrayList<>();
        int maxLines = 35;
        for (String rawLine : rawLines) {
            String safeLine = toAscii(rawLine == null ? "" : rawLine);
            if (safeLine.length() > 110) {
                lines.add(safeLine.substring(0, 110));
            } else {
                lines.add(safeLine);
            }
            if (lines.size() >= maxLines) {
                lines.set(maxLines - 1, "...(truncated)");
                break;
            }
        }
        if (lines.isEmpty()) {
            lines.add("Empty report content");
        }
        return lines;
    }

    private String toAscii(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            builder.append(current <= 0x7F ? current : '?');
        }
        return builder.toString();
    }

    private String escapePdfText(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }
}

