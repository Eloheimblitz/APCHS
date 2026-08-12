package com.airpollution.survey.service;

import com.airpollution.survey.entity.SurveyRecord;
import com.opencsv.CSVWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
    private static final String[] HEADERS = {
            "Survey ID", "Survey Date", "Household ID", "Submitted By", "Study Area", "Age", "Gender",
            "Cooking", "Wood/Coal Cooking Location", "Symptoms Summary", "Conditions Summary",
            "Distance To Highway", "Distance To Factory", "Grid ID", "Latitude", "Longitude", "GPS Accuracy",
            "Remarks"
    };

    private final SurveyService surveyService;
    private final SurveyMapper mapper;

    public ExportService(SurveyService surveyService, SurveyMapper mapper) {
        this.surveyService = surveyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public byte[] csv(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = filtered(filters, authentication);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            writer.writeNext(HEADERS);
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
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            for (int r = 0; r < records.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = row(records.get(r));
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }
            for (int i = 0; i < HEADERS.length; i++) {
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
        return new String[] {
                text(r.getSurveyId()), text(r.getSurveyDate()), text(r.getHouseholdId()), text(r.getSubmittedBy()),
                label(r.getStudyArea()), text(r.getAge()), label(r.getGender()),
                label(r.getPrimaryCookingFuel()), label(r.getWoodCoalCookingLocation()),
                mapper.summarize(r.getSymptoms()), mapper.summarize(r.getConditions()),
                text(r.getDistanceToHighway()), text(r.getDistanceToFactory()), text(r.getGridId()),
                text(r.getLatitude()), text(r.getLongitude()), text(r.getGpsAccuracy()),
                text(r.getRemarks())
        };
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String label(String value) {
        return value == null ? "" : value.replace('_', ' ');
    }
}
