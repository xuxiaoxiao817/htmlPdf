package com.example.agreement.controller;

import com.example.agreement.dto.AgreementRequest;
import com.example.agreement.dto.LanguageOption;
import com.example.agreement.entity.Agreement;
import com.example.agreement.service.AgreementService;
import com.example.agreement.service.LanguageService;
import com.example.agreement.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;
    private final PdfService pdfService;
    private final LanguageService languageService;

    @GetMapping("/languages")
    public List<LanguageOption> languages() {
        return languageService.all();
    }

    @GetMapping("/agreements")
    public List<Agreement> list() {
        return agreementService.list();
    }

    @GetMapping("/agreements/{id}")
    public Agreement get(@PathVariable Long id) {
        return agreementService.get(id);
    }

    @PostMapping("/agreements")
    public Agreement create(@Valid @RequestBody AgreementRequest req) {
        return agreementService.create(req);
    }

    @DeleteMapping("/agreements/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agreementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agreements/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Agreement a = agreementService.get(id);
        byte[] pdf = pdfService.renderPdf(a.getContent(), a.getLanguage(), a.getTitle());
        String filename = pdfService.suggestedFileName(a.getId(), a.getLanguage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(pdfService.contentType()));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
