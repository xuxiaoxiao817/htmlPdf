package com.example.agreement.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

    private static final String CSS_RESOURCE = "fonts/noto.css";
    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="%s">
            <head>
              <meta charset="UTF-8"/>
              <title>Agreement</title>
              <style>%s</style>
            </head>
            <body>
              <div class="content">%s</div>
            </body>
            </html>
            """;

    public byte[] renderPdf(String htmlContent, String language, String title) {
        if (htmlContent == null) {
            throw new IllegalArgumentException("content is empty");
        }

        String css = readClasspathFile(CSS_RESOURCE);
        String safeTitle = (title == null || title.isBlank()) ? "Agreement" : escapeXml(title);
        String fullHtml = HTML_TEMPLATE.formatted(
                language == null ? "en" : language,
                css,
                htmlContent
        );

        ConverterProperties props = new ConverterProperties();
        props.setCharset("UTF-8");
        try {
            props.setBaseUri(ResourceUtils.getURL(ResourceUtils.CLASSPATH_URL_PREFIX).toString());
        } catch (IOException e) {
            log.warn("cannot resolve classpath baseUri, font URLs may fail", e);
        }

        DefaultFontProvider fontProvider = new DefaultFontProvider(true, true, true);
        // Register every bundled font. The CSS @font-face with unicode-range
        // is what actually drives per-codepoint font selection, but we register
        // the binary data here so pdfHTML has access if it needs to look fonts up
        // by name (e.g. when CSS resolution is bypassed).
        registerFontSafely(fontProvider, "fonts/NotoSans-Regular.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSans-Bold.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSans-Italic.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSans-BoldItalic.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansThai-Regular.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansThai-Bold.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansHebrew-Regular.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansHebrew-Bold.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansArabic-Regular.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansArabic-Bold.ttf");
        registerFontSafely(fontProvider, "fonts/NotoSansCJKsc-Regular.otf");
        registerFontSafely(fontProvider, "fonts/NotoSansCJKtc-Regular.otf");
        registerFontSafely(fontProvider, "fonts/NotoSansCJKjp-Regular.otf");
        registerFontSafely(fontProvider, "fonts/NotoSansCJKkr-Regular.otf");
        props.setFontProvider(fontProvider);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer)) {
            pdf.setDefaultPageSize(PageSize.A4);
            HtmlConverter.convertToPdf(fullHtml, pdf, props);
        } catch (Exception e) {
            log.error("PDF render failed (title='{}', lang={})", safeTitle, language, e);
            throw new RuntimeException("failed to render PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    public String suggestedFileName(Long id, String language) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "agreement_%d_%s_%s.pdf".formatted(id, language == null ? "und" : language, date);
    }

    public String contentType() {
        return "application/pdf";
    }

    private void registerFontSafely(DefaultFontProvider provider, String classpathPath) {
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            provider.addFont(bytes);
            log.debug("registered font: {}", classpathPath);
        } catch (IOException e) {
            log.warn("font not available, skipping: {}", classpathPath);
        }
    }

    private String readClasspathFile(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("missing classpath resource: " + path, e);
        }
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

}
