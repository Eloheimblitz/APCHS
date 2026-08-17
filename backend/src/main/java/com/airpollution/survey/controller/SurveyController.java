package com.airpollution.survey.controller;

import com.airpollution.survey.dto.SurveyCreateRequest;
import com.airpollution.survey.dto.SurveyResponse;
import com.airpollution.survey.dto.SurveyUpdateRequest;
import com.airpollution.survey.service.SurveyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping
    public SurveyResponse create(@Valid @RequestBody SurveyCreateRequest request, Authentication authentication) {
        return surveyService.create(request, authentication);
    }

    @GetMapping("/next-id")
    public Map<String, String> nextId() {
        return Map.of("surveyId", surveyService.suggestNextSurveyId());
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
