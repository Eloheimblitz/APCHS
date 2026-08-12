package com.airpollution.survey.service;

import com.airpollution.survey.dto.SurveyPayload;
import com.airpollution.survey.dto.SurveyResponse;
import com.airpollution.survey.entity.HealthItemEntry;
import com.airpollution.survey.entity.SurveyRecord;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class SurveyMapper {
    public void copyPayload(SurveyPayload payload, SurveyRecord record) {
        BeanUtils.copyProperties(payload, record, "id", "surveyId", "householdId", "submittedBy",
                "createdAt", "updatedAt");
    }

    public SurveyResponse toResponse(SurveyRecord r) {
        return new SurveyResponse(
                r.getId(), r.getSurveyId(), r.getHouseholdId(), r.getSubmittedBy(), r.getCreatedAt(), r.getUpdatedAt(),
                r.getSurveyDate(), r.getSurveyorId(), r.getConsentObtained(), r.getStudyArea(),
                r.getLatitude(), r.getLongitude(), r.getGpsAccuracy(), r.getGridId(), r.getDistanceToHighway(), r.getDistanceToFactory(),
                r.getAge(), r.getDurationOfStayAtStudyArea(), r.getGender(), r.getTobaccoUse(), r.getAlcohol(),
                r.getEthnicity(), r.getOtherEthnicity(), r.getEducation(), r.getOtherEducation(),
                r.getOccupation(), r.getOtherOccupation(),
                r.getPrimaryCookingFuel(), r.getWoodCoalCookingLocation(),
                r.getHasChildren(), r.getNumberOfChildren(), r.getChildBirthplace(), r.getChildVaccination(),
                r.getRespondentVaccination(), r.getMhisSmartCard(),
                r.getConditions(), r.getCancerType(),
                r.getSymptoms(), r.getFeverDuration(),
                r.getOtherIssues(),
                r.getRemarks(),
                summarize(r.getSymptoms())
        );
    }

    public String summarize(List<HealthItemEntry> items) {
        if (items == null || items.isEmpty()) return "None reported";
        StringBuilder builder = new StringBuilder();
        for (HealthItemEntry entry : items) {
            if (Boolean.TRUE.equals(entry.getPresent())) {
                if (!builder.isEmpty()) builder.append(", ");
                builder.append(label(entry.getKey()));
            }
        }
        return builder.isEmpty() ? "None reported" : builder.toString();
    }

    public String label(String key) {
        if (key == null || key.isBlank()) return "";
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
