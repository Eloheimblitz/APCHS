package com.airpollution.survey.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalHouseholdsSurveyed,
        long totalStudyAreasCovered,
        long householdsUsingWoodFirewood,
        long smokers,
        long respondentsWithRespiratorySymptoms,
        long hospitalVisits,
        BigDecimal averageMissedWorkSchoolDays,
        Map<String, Long> surveyCountByStudyArea,
        Map<String, Long> cookingFuelDistribution,
        Map<String, Long> commonSymptomsCount,
        Map<String, Long> hospitalVisitDistribution
) {
}
