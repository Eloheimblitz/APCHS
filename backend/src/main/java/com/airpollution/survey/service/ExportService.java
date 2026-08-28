package com.airpollution.survey.service;

import com.airpollution.survey.entity.HealthItemEntry;
import com.airpollution.survey.entity.SurveyRecord;
import com.opencsv.CSVWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
    private static final String[] BASE_HEADERS = {
            "Survey ID", "Survey Date", "Household ID", "Submitted By", "Surveyor ID", "Consent Obtained",
            "Study Area", "Latitude", "Longitude", "GPS Accuracy", "Grid ID", "Distance To Highway", "Distance To Factory",
            "Age", "Duration Of Stay At Study Area", "Gender", "Tobacco Use", "Alcohol",
            "Ethnicity", "Other Ethnicity", "Education", "Other Education", "Occupation", "Other Occupation",
            "Cooking", "Wood/Coal Cooking Location",
            "Has Children", "Number Of Children", "Child Birthplace", "Child Vaccination",
            "Respondent Vaccination", "MHIS Smart Card", "Cancer Type",
            "Worried About Air Pollution", "Air Pollution Concerns", "Other Air Pollution Concern",
            "Fever Duration", "Remarks"
    };

    private static final String[] ITEM_SUFFIXES = {
            "Present", "Visited Hospital", "Hospital Name(s)", "IPD", "OPD", "Missed School/Work", "Days Missed"
    };

    private static final String[] OTHER_ISSUE_SUFFIXES = {
            "Description", "Visited Hospital", "Hospital Name(s)", "IPD", "OPD", "Missed School/Work", "Days Missed"
    };

    private static final int OTHER_ISSUE_SLOTS = 5;

    private final SurveyService surveyService;
    private final SurveyMapper mapper;
    private final String[] headers;

    public ExportService(SurveyService surveyService, SurveyMapper mapper) {
        this.surveyService = surveyService;
        this.mapper = mapper;
        this.headers = buildHeaders();
    }

    private String[] buildHeaders() {
        List<String> all = new ArrayList<>(List.of(BASE_HEADERS));
        for (String key : SurveyCatalog.SYMPTOM_KEYS) {
            for (String suffix : ITEM_SUFFIXES) {
                all.add(mapper.label(key) + " - " + suffix);
            }
        }
        for (String key : SurveyCatalog.CONDITION_KEYS) {
            for (String suffix : ITEM_SUFFIXES) {
                all.add(mapper.label(key) + " - " + suffix);
            }
        }
        for (int i = 1; i <= OTHER_ISSUE_SLOTS; i++) {
            for (String suffix : OTHER_ISSUE_SUFFIXES) {
                all.add("Other Issue " + i + " - " + suffix);
            }
        }
        return all.toArray(String[]::new);
    }

    @Transactional(readOnly = true)
    public byte[] csv(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = filtered(filters, authentication);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            writer.writeNext(headers);
            for (SurveyRecord record : records) {
                writer.writeNext(row(record));
            }
            writer.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to export CSV", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] xlsx(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = filtered(filters, authentication);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Survey Records");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 0; r < records.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = row(records.get(r));
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to export Excel", e);
        }
    }

    private List<SurveyRecord> filtered(Map<String, String> filters, Authentication authentication) {
        return surveyService.findFiltered(filters, authentication);
    }

    private String[] row(SurveyRecord r) {
        List<String> values = new ArrayList<>(List.of(
                text(r.getSurveyId()), text(r.getSurveyDate()), text(r.getHouseholdId()), text(r.getSubmittedBy()),
                text(r.getSurveyorId()), bool(r.getConsentObtained()),
                label(r.getStudyArea()), text(r.getLatitude()), text(r.getLongitude()), text(r.getGpsAccuracy()),
                text(r.getGridId()), text(r.getDistanceToHighway()), text(r.getDistanceToFactory()),
                text(r.getAge()), text(r.getDurationOfStayAtStudyArea()), label(r.getGender()),
                labelList(r.getTobaccoUse()), bool(r.getAlcohol()),
                label(r.getEthnicity()), text(r.getOtherEthnicity()), label(r.getEducation()), text(r.getOtherEducation()),
                labelList(r.getOccupation()), text(r.getOtherOccupation()),
                labelList(r.getPrimaryCookingFuel()), labelList(r.getWoodCoalCookingLocation()),
                bool(r.getHasChildren()), text(r.getNumberOfChildren()), labelList(r.getChildBirthplace()), label(r.getChildVaccination()),
                bool(r.getRespondentVaccination()), bool(r.getMhisSmartCard()), text(r.getCancerType()),
                bool(r.getWorriedAboutAirPollution()), labelList(r.getAirPollutionConcerns()), text(r.getOtherAirPollutionConcern()),
                label(r.getFeverDuration()),
                text(r.getRemarks())
        ));

        Map<String, HealthItemEntry> symptomsByKey = byKey(r.getSymptoms());
        Map<String, HealthItemEntry> conditionsByKey = byKey(r.getConditions());
        for (String key : SurveyCatalog.SYMPTOM_KEYS) {
            appendItem(values, symptomsByKey.get(key));
        }
        for (String key : SurveyCatalog.CONDITION_KEYS) {
            appendItem(values, conditionsByKey.get(key));
        }
        List<HealthItemEntry> otherIssues = r.getOtherIssues();
        for (int i = 0; i < OTHER_ISSUE_SLOTS; i++) {
            HealthItemEntry item = otherIssues != null && i < otherIssues.size() ? otherIssues.get(i) : null;
            values.add(item == null ? "" : text(item.getDescription()));
            appendVisitDetail(values, item);
        }

        return values.toArray(String[]::new);
    }

    private void appendItem(List<String> values, HealthItemEntry item) {
        values.add(item == null ? "" : bool(item.getPresent()));
        appendVisitDetail(values, item);
    }

    private void appendVisitDetail(List<String> values, HealthItemEntry item) {
        values.add(item == null ? "" : bool(item.getVisitedHospital()));
        values.add(item == null ? "" : text(item.getHospitalNames()));
        values.add(item == null ? "" : bool(item.getIpd()));
        values.add(item == null ? "" : bool(item.getOpd()));
        values.add(item == null ? "" : bool(item.getMissedSchoolOrWork()));
        values.add(item == null ? "" : text(item.getDaysMissed()));
    }

    private Map<String, HealthItemEntry> byKey(List<HealthItemEntry> items) {
        if (items == null) return Map.of();
        return items.stream().collect(Collectors.toMap(HealthItemEntry::getKey, entry -> entry, (a, b) -> a));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String bool(Boolean value) {
        if (value == null) return "";
        return value ? "Yes" : "No";
    }

    private String label(String value) {
        return value == null ? "" : value.replace('_', ' ');
    }

    private String labelList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(this::label).collect(Collectors.joining(", "));
    }
}
