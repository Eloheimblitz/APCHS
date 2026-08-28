package com.airpollution.survey.service;

import com.airpollution.survey.dto.DashboardSummaryResponse;
import com.airpollution.survey.entity.HealthItemEntry;
import com.airpollution.survey.entity.SurveyRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final Set<String> RESPIRATORY_SYMPTOM_KEYS = Set.of(
            "DRY_COUGH", "WET_COUGH", "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT"
    );
    private static final List<String> COMMON_SYMPTOM_KEYS = List.of(
            "DRY_COUGH", "WET_COUGH", "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT", "EYE_ITCHING"
    );

    private final SurveyService surveyService;

    public DashboardService(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Authentication authentication) {
        List<SurveyRecord> records = surveyService.findFiltered(Map.of(), authentication);
        long totalHouseholds = records.size();
        long studyAreas = records.stream().map(SurveyRecord::getStudyArea).filter(v -> v != null && !v.isBlank()).distinct().count();
        long wood = records.stream().filter(r -> r.getPrimaryCookingFuel() != null && r.getPrimaryCookingFuel().contains("WOOD")).count();
        long smokers = records.stream().filter(r -> r.getTobaccoUse() != null && r.getTobaccoUse().contains("SMOKING")).count();

        long respiratorySymptoms = 0;
        long hospitalVisits = 0;
        Map<String, Long> commonSymptoms = new LinkedHashMap<>();
        for (String key : COMMON_SYMPTOM_KEYS) commonSymptoms.put(key, 0L);
        List<Integer> perRecordMissedDays = new ArrayList<>();
        Map<String, Long> hospitalVisitCounts = new LinkedHashMap<>(Map.of("Yes", 0L, "No", 0L));

        for (SurveyRecord record : records) {
            List<HealthItemEntry> allItems = allItems(record);
            if (hasAny(allItems, RESPIRATORY_SYMPTOM_KEYS)) respiratorySymptoms++;
            boolean visitedHospital = allItems.stream().anyMatch(entry -> Boolean.TRUE.equals(entry.getVisitedHospital()));
            if (visitedHospital) hospitalVisits++;
            hospitalVisitCounts.merge(visitedHospital ? "Yes" : "No", 1L, Long::sum);
            for (String key : COMMON_SYMPTOM_KEYS) {
                if (HealthItemEntry.isPresent(record.getSymptoms(), key)) {
                    commonSymptoms.merge(key, 1L, Long::sum);
                }
            }
            List<Integer> daysMissed = allItems.stream()
                    .map(HealthItemEntry::getDaysMissed)
                    .filter(v -> v != null && v >= 0)
                    .toList();
            if (!daysMissed.isEmpty()) {
                perRecordMissedDays.add(daysMissed.stream().mapToInt(Integer::intValue).sum());
            }
        }

        return new DashboardSummaryResponse(
                totalHouseholds,
                studyAreas,
                wood,
                smokers,
                respiratorySymptoms,
                hospitalVisits,
                averageMissedDays(perRecordMissedDays),
                countBy(records, SurveyRecord::getStudyArea),
                countByMulti(records, SurveyRecord::getPrimaryCookingFuel),
                commonSymptoms,
                hospitalVisitCounts
        );
    }

    private Map<String, Long> countBy(List<SurveyRecord> records, Function<SurveyRecord, String> getter) {
        return records.stream()
                .collect(Collectors.groupingBy(r -> valueOrUnknown(getter.apply(r)), LinkedHashMap::new, Collectors.counting()));
    }

    private Map<String, Long> countByMulti(List<SurveyRecord> records, Function<SurveyRecord, List<String>> getter) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (SurveyRecord record : records) {
            List<String> values = getter.apply(record);
            if (values == null || values.isEmpty()) {
                counts.merge("UNKNOWN", 1L, Long::sum);
                continue;
            }
            for (String value : values) {
                counts.merge(valueOrUnknown(value), 1L, Long::sum);
            }
        }
        return counts;
    }

    private boolean hasAny(List<HealthItemEntry> items, Set<String> keys) {
        return items.stream().anyMatch(entry -> entry.getKey() != null && keys.contains(entry.getKey()) && Boolean.TRUE.equals(entry.getPresent()));
    }

    private List<HealthItemEntry> allItems(SurveyRecord record) {
        List<HealthItemEntry> all = new ArrayList<>();
        if (record.getSymptoms() != null) all.addAll(record.getSymptoms());
        if (record.getConditions() != null) all.addAll(record.getConditions());
        if (record.getOtherIssues() != null) all.addAll(record.getOtherIssues());
        return all;
    }

    private BigDecimal averageMissedDays(List<Integer> perRecordTotals) {
        if (perRecordTotals.isEmpty()) return BigDecimal.ZERO;
        int sum = perRecordTotals.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(perRecordTotals.size()), 2, RoundingMode.HALF_UP);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
