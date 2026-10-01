package com.airpollution.survey.service;

import com.airpollution.survey.dto.DashboardSummaryResponse;
import com.airpollution.survey.entity.SurveyRecord;
import com.airpollution.survey.entity.HealthItemEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final List<String> COMMON_SYMPTOM_KEYS = List.of(
            "DRY_COUGH", "WET_COUGH", "WHEEZING", "BREATHLESSNESS", "CHEST_DISCOMFORT", "EYE_ITCHING"
    );

    private final SurveyService surveyService;

    public DashboardService(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = surveyService.findFiltered(filters, authentication);
        if (hasValue(filters.get("gender"))) {
            records = records.stream().filter(r -> filters.get("gender").equalsIgnoreCase(r.getGender())).toList();
        }
        if (hasValue(filters.get("ageGroup"))) {
            records = records.stream().filter(r -> filters.get("ageGroup").equals(ageBucket(r.getAge()))).toList();
        }
        long totalHouseholds = records.size();
        long studyAreas = records.stream().map(SurveyRecord::getStudyArea).filter(v -> v != null && !v.isBlank()).distinct().count();

        Map<String, Long> commonSymptoms = seeded(COMMON_SYMPTOM_KEYS);
        Map<String, Long> conditionsCount = seeded(List.of(SurveyCatalog.CONDITION_KEYS));
        for (SurveyRecord record : records) {
            for (String key : COMMON_SYMPTOM_KEYS) {
                if (HealthItemEntry.isPresent(record.getSymptoms(), key)) {
                    commonSymptoms.merge(key, 1L, Long::sum);
                }
            }
            for (String key : SurveyCatalog.CONDITION_KEYS) {
                if (HealthItemEntry.isPresent(record.getConditions(), key)) {
                    conditionsCount.merge(key, 1L, Long::sum);
                }
            }
        }

        return new DashboardSummaryResponse(
                totalHouseholds,
                studyAreas,
                countBy(records, SurveyRecord::getStudyArea),
                countByOrdered(records, SurveyRecord::getGender, "MALE", "FEMALE"),
                countByOrdered(records, r -> ageBucket(r.getAge()), "Under 18", "18-34", "35-49", "50-64", "65+"),
                countByOrdered(records, SurveyRecord::getChildVaccination, "YES", "NO", "NA"),
                countByOrdered(records, SurveyRecord::getRespondentVaccination, "YES", "NO", "NA"),
                conditionsCount,
                commonSymptoms
        );
    }

    private Map<String, Long> seeded(List<String> keys) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : keys) counts.put(key, 0L);
        return counts;
    }

    private String ageBucket(Integer age) {
        if (age == null) return "UNKNOWN";
        if (age < 18) return "Under 18";
        if (age < 35) return "18-34";
        if (age < 50) return "35-49";
        if (age < 65) return "50-64";
        return "65+";
    }

    private Map<String, Long> countBy(List<SurveyRecord> records, Function<SurveyRecord, String> getter) {
        return records.stream()
                .collect(Collectors.groupingBy(r -> valueOrUnknown(getter.apply(r)), LinkedHashMap::new, Collectors.counting()));
    }

    private Map<String, Long> countByOrdered(List<SurveyRecord> records, Function<SurveyRecord, String> getter,
                                              String... orderedKeys) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : orderedKeys) counts.put(key, 0L);
        for (SurveyRecord record : records) {
            counts.merge(valueOrUnknown(getter.apply(record)), 1L, Long::sum);
        }
        return counts;
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
