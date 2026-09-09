package com.airpollution.survey.controller;

import com.airpollution.survey.dto.SurveyCreateRequest;
import com.airpollution.survey.dto.SurveyResponse;
import com.airpollution.survey.dto.SurveyUpdateRequest;
import com.airpollution.survey.entity.SurveyRecord;
import com.airpollution.survey.service.PdfExportService;
import com.airpollution.survey.service.SurveyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {
    private final SurveyService surveyService;
    private final PdfExportService pdfExportService;

    public SurveyController(SurveyService surveyService, PdfExportService pdfExportService) {
        this.surveyService = surveyService;
        this.pdfExportService = pdfExportService;
    }

    @PostMapping
    public SurveyResponse create(@Valid @RequestBody SurveyCreateRequest request, Authentication authentication) {
        return surveyService.create(request, authentication);
    }

    @GetMapping("/check-id")
    public Map<String, Boolean> checkId(@RequestParam String surveyId) {
        return Map.of("exists", surveyService.surveyIdExists(surveyId));
    }

    @GetMapping
    public List<SurveyResponse> list(@RequestParam Map<String, String> filters, Authentication authentication) {
        return surveyService.list(filters, authentication);
    }

    @GetMapping("/{id}")
    public SurveyResponse get(@PathVariable Long id, Authentication authentication) {
        return surveyService.get(id, authentication);
    }

    @GetMapping("/{id}/export.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id, Authentication authentication) {
        SurveyRecord record = surveyService.getRecord(id, authentication);
        byte[] pdf = pdfExportService.generate(record);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(record.getSurveyId() + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PutMapping("/{id}")
    public SurveyResponse update(@PathVariable Long id, @Valid @RequestBody SurveyUpdateRequest request,
                                 Authentication authentication) {
        return surveyService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        surveyService.delete(id, authentication);
    }
}
