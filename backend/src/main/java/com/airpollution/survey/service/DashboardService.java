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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final String[] RESPIRATORY_SYMPTOM_KEYS = {
            "DRY_COUGH", "WET_COUGH", "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT"
    };
    private static final String[] COMMON_SYMPTOM_KEYS = {
            "DRY_COUGH", "WET_COUGH", "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT", "EYE_IRRITATION"
    };

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
        long respiratorySymptoms = records.stream().filter(this::hasRespiratorySymptoms).count();
        long hospitalVisits = records.stream().filter(this::hasAnyHospitalVisit).count();
        BigDecimal averageMissedDays = averageMissedDays(records);

        return new DashboardSummaryResponse(
                totalHouseholds,
                studyAreas,
                wood,
                smokers,
                respiratorySymptoms,
                hospitalVisits,
                averageMissedDays,
                countBy(records, SurveyRecord::getStudyArea),
                countByMulti(records, SurveyRecord::getPrimaryCookingFuel),
                commonSymptoms(records),
                records.stream().collect(Collectors.groupingBy(r -> hasAnyHospitalVisit(r) ? "Yes" : "No",
                        LinkedHashMap::new, Collectors.counting()))
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

    private Map<String, Long> commonSymptoms(List<SurveyRecord> records) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : COMMON_SYMPTOM_KEYS) {
            counts.put(key, records.stream().filter(r -> isPresent(r.getSymptoms(), key)).count());
        }
        return counts;
    }

    private boolean hasRespiratorySymptoms(SurveyRecord r) {
        for (String key : RESPIRATORY_SYMPTOM_KEYS) {
            if (isPresent(r.getSymptoms(), key)) return true;
        }
        return false;
    }

    private boolean isPresent(List<HealthItemEntry> items, String key) {
        if (items == null) return false;
        return items.stream().anyMatch(entry -> key.equals(entry.getKey()) && Boolean.TRUE.equals(entry.getPresent()));
    }

    private boolean hasAnyHospitalVisit(SurveyRecord record) {
        return allItems(record).anyMatch(entry -> Boolean.TRUE.equals(entry.getVisitedHospital()));
    }

    private Stream<HealthItemEntry> allItems(SurveyRecord record) {
        List<HealthItemEntry> all = new ArrayList<>();
        if (record.getSymptoms() != null) all.addAll(record.getSymptoms());
        if (record.getConditions() != null) all.addAll(record.getConditions());
        if (record.getOtherIssues() != null) all.addAll(record.getOtherIssues());
        return all.stream();
    }

    private BigDecimal averageMissedDays(List<SurveyRecord> records) {
        List<Integer> perRecordTotals = new ArrayList<>();
        for (SurveyRecord record : records) {
            List<Integer> daysMissed = allItems(record)
                    .map(HealthItemEntry::getDaysMissed)
                    .filter(v -> v != null && v >= 0)
                    .toList();
            if (!daysMissed.isEmpty()) {
                perRecordTotals.add(daysMissed.stream().mapToInt(Integer::intValue).sum());
            }
        }
        if (perRecordTotals.isEmpty()) return BigDecimal.ZERO;
        int sum = perRecordTotals.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(perRecordTotals.size()), 2, RoundingMode.HALF_UP);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
