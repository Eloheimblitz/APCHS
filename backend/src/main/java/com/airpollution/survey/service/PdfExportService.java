package com.airpollution.survey.service;

import static com.airpollution.survey.service.TextFormat.bool;
import static com.airpollution.survey.service.TextFormat.label;
import static com.airpollution.survey.service.TextFormat.labelList;
import static com.airpollution.survey.service.TextFormat.text;

import com.airpollution.survey.entity.HealthItemEntry;
import com.airpollution.survey.entity.SurveyRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

@Service
public class PdfExportService {
    private final SurveyMapper mapper;

    public PdfExportService(SurveyMapper mapper) {
        this.mapper = mapper;
    }

    public byte[] generate(SurveyRecord r) {
        try (PDDocument doc = new PDDocument()) {
            Writer w = new Writer(doc);

            w.title("Air Pollution & Community Health Survey");
            w.subtitle("Survey ID: " + text(r.getSurveyId()) + "   |   Household ID: " + text(r.getHouseholdId()));
            w.gap(8);

            w.section("A. Survey Information");
            w.field("Survey Date", text(r.getSurveyDate()));
            w.field("Surveyor Name", text(r.getSurveyorId()));
            w.field("Consent Obtained", bool(r.getConsentObtained()));
            w.field("Study Area", label(r.getStudyArea()));
            w.field("Grid ID", text(r.getGridId()));
            w.field("Latitude", text(r.getLatitude()));
            w.field("Longitude", text(r.getLongitude()));
            w.field("GPS Accuracy (m)", text(r.getGpsAccuracy()));
            w.field("Distance To Highway", text(r.getDistanceToHighway()));
            w.field("Distance To Factory", text(r.getDistanceToFactory()));

            w.section("B. Demographics");
            w.field("Age", text(r.getAge()));
            w.field("Duration Of Stay At Study Area", text(r.getDurationOfStayAtStudyArea()));
            w.field("Gender", label(r.getGender()));
            w.field("Tobacco", labelList(r.getTobaccoUse()));
            w.field("Alcohol", bool(r.getAlcohol()));
            w.field("Ethnicity", label(r.getEthnicity()));
            w.field("Other Ethnicity", text(r.getOtherEthnicity()));
            w.field("Education", label(r.getEducation()));
            w.field("Other Education", text(r.getOtherEducation()));
            w.field("Occupation", labelList(r.getOccupation()));
            w.field("Other Occupation", text(r.getOtherOccupation()));

            w.section("C. Cooking");
            w.field("Cooking", labelList(r.getPrimaryCookingFuel()));
            w.field("Wood/Coal Cooking Location", labelList(r.getWoodCoalCookingLocation()));

            w.section("D. Children and Vaccination");
            w.field("Do you have children?", bool(r.getHasChildren()));
            w.field("Number Of Children", text(r.getNumberOfChildren()));
            w.field("Child Birthplace", labelList(r.getChildBirthplace()));
            w.field("Child Vaccination", label(r.getChildVaccination()));
            w.field("Respondent Vaccination", label(r.getRespondentVaccination()));
            w.field("MHIS/Smart Card", bool(r.getMhisSmartCard()));

            w.section("E. Existing Health Conditions");
            w.healthTable(SurveyCatalog.CONDITION_KEYS, r.getConditions(), mapper);
            w.field("Type of Cancer", text(r.getCancerType()));
            w.field("Worried about air pollution?", bool(r.getWorriedAboutAirPollution()));
            w.field("What are you worried about?", labelList(r.getAirPollutionConcerns()));
            w.field("Other concern", text(r.getOtherAirPollutionConcern()));

            w.section("F. Symptoms (last 2 months)");
            w.healthTable(SurveyCatalog.SYMPTOM_KEYS, r.getSymptoms(), mapper);
            w.field("Fever Duration", label(r.getFeverDuration()));

            w.section("G. Other Issues");
            w.otherIssues(r.getOtherIssues());
            w.field("Remarks", text(r.getRemarks()));

            w.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate PDF", e);
        }
    }

    private static final class Writer {
        private static final float MARGIN = 50;
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
        private static final float LINE_HEIGHT = 14;
        private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
        private static final PDFont REGULAR = PDType1Font.HELVETICA;

        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;

        Writer(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) newPage();
        }

        void title(String value) throws IOException {
            ensureSpace(20);
            draw(BOLD, 16, value);
            y -= 22;
        }

        void subtitle(String value) throws IOException {
            ensureSpace(14);
            draw(REGULAR, 10, value);
            y -= 16;
        }

        void gap(float amount) {
            y -= amount;
        }

        void section(String value) throws IOException {
            ensureSpace(26);
            y -= 6;
            draw(BOLD, 12, value);
            y -= 6;
            stream.setLineWidth(0.5f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(PAGE_WIDTH - MARGIN, y);
            stream.stroke();
            y -= 14;
        }

        void field(String fieldLabel, String value) throws IOException {
            String display = value == null || value.isBlank() ? "-" : value;
            for (String line : wrap(fieldLabel + ": " + display, REGULAR, 10, CONTENT_WIDTH)) {
                ensureSpace(LINE_HEIGHT);
                draw(REGULAR, 10, line);
                y -= LINE_HEIGHT;
            }
        }

        void healthTable(String[] keys, List<HealthItemEntry> items, SurveyMapper mapper) throws IOException {
            for (String key : keys) {
                HealthItemEntry item = HealthItemEntry.findByKey(items, key);
                boolean present = item != null && Boolean.TRUE.equals(item.getPresent());
                String line = mapper.label(key) + ": " + (present ? "Yes" : "No");
                if (present) {
                    String details = visitDetails(item);
                    if (!details.isEmpty()) line += " (" + details + ")";
                }
                for (String wrapped : wrap(line, REGULAR, 10, CONTENT_WIDTH)) {
                    ensureSpace(LINE_HEIGHT);
                    draw(REGULAR, 10, wrapped);
                    y -= LINE_HEIGHT;
                }
            }
        }

        void otherIssues(List<HealthItemEntry> items) throws IOException {
            boolean any = false;
            if (items != null) {
                for (HealthItemEntry item : items) {
                    if (item.getDescription() == null || item.getDescription().isBlank()) continue;
                    any = true;
                    String line = "- " + item.getDescription();
                    String details = visitDetails(item);
                    if (!details.isEmpty()) line += " (" + details + ")";
                    for (String wrapped : wrap(line, REGULAR, 10, CONTENT_WIDTH)) {
                        ensureSpace(LINE_HEIGHT);
                        draw(REGULAR, 10, wrapped);
                        y -= LINE_HEIGHT;
                    }
                }
            }
            if (!any) field("Other Issues", "None reported");
        }

        private String visitDetails(HealthItemEntry item) {
            List<String> details = new ArrayList<>();
            if (Boolean.TRUE.equals(item.getVisitedHospital())) details.add("Visited hospital");
            if (item.getHospitalNames() != null && !item.getHospitalNames().isBlank()) {
                details.add("Hospital: " + item.getHospitalNames());
            }
            if (Boolean.TRUE.equals(item.getIpd())) details.add("IPD");
            if (Boolean.TRUE.equals(item.getOpd())) details.add("OPD");
            if (Boolean.TRUE.equals(item.getMissedSchoolOrWork())) details.add("Missed school/work");
            if (item.getDaysMissed() != null) details.add(item.getDaysMissed() + " day(s) missed");
            return String.join(", ", details);
        }

        private void draw(PDFont font, float size, String value) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(value));
            stream.endText();
        }

        private String sanitize(String value) {
            StringBuilder sb = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                sb.append(c < 256 ? c : '?');
            }
            return sb.toString();
        }

        private List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : value.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(sanitize(candidate)) / 1000 * size;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
            return lines;
        }

        void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
