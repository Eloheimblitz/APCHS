package com.airpollution.survey.dto;

import java.util.Map;

public record DashboardSummaryResponse(
        long totalHouseholdsSurveyed,
        long totalStudyAreasCovered,
        Map<String, Long> surveyCountByStudyArea,
        Map<String, Long> genderDistribution,
        Map<String, Long> ageDistribution,
        Map<String, Long> childVaccinationDistribution,
        Map<String, Long> respondentVaccinationDistribution,
        Map<String, Long> conditionsCount,
        Map<String, Long> commonSymptomsCount
) {
}
