package com.airpollution.survey.controller;

import com.airpollution.survey.service.ExportService;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/surveys.csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> csv(@RequestParam Map<String, String> filters, Authentication authentication) {
        return file(exportService.csv(filters, authentication), "surveys.csv", "text/csv");
    }

    @GetMapping("/surveys.xlsx")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> xlsx(@RequestParam Map<String, String> filters, Authentication authentication) {
        return file(exportService.xlsx(filters, authentication), "surveys.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/surveys.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> pdf(@RequestParam Map<String, String> filters, Authentication authentication) {
        return file(exportService.pdf(filters, authentication), "surveys.pdf", "application/pdf");
    }

    private ResponseEntity<byte[]> file(byte[] bytes, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }
}
