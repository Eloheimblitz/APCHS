package com.airpollution.survey.dto;

import java.util.List;

public record SurveyPageResponse(
        List<SurveyResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
