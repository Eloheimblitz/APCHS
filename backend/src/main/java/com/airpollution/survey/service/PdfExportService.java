package com.airpollution.survey.service;

import static com.airpollution.survey.service.TextFormat.bool;
import static com.airpollution.survey.service.TextFormat.label;
import static com.airpollution.survey.service.TextFormat.labelList;
import static com.airpollution.survey.service.TextFormat.text;

import com.airpollution.survey.entity.HealthItemEntry;
import com.airpollution.survey.entity.SurveyRecord;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
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
            w.header("Air Pollution & Community Health Survey", text(r.getSurveyId()), text(r.getHouseholdId()),
                    text(r.getSurveyDate()));

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
            w.healthTable("Condition", SurveyCatalog.CONDITION_KEYS, r.getConditions(), mapper);
            w.field("Type of Cancer", text(r.getCancerType()));
            w.field("Worried about air pollution?", bool(r.getWorriedAboutAirPollution()));
            w.field("What are you worried about?", labelList(r.getAirPollutionConcerns()));
            w.field("Other concern", text(r.getOtherAirPollutionConcern()));

            w.section("F. Symptoms (last 2 months)");
            w.healthTable("Symptom", SurveyCatalog.SYMPTOM_KEYS, r.getSymptoms(), mapper);
            w.field("Fever Duration", label(r.getFeverDuration()));

            w.section("G. Other Issues");
            w.otherIssues(r.getOtherIssues());
            w.field("Remarks", text(r.getRemarks()));

            w.close();
            w.paginate();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate PDF", e);
        }
    }

    private static final class Writer {
        private static final float MARGIN = 42;
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
        private static final float BOX_WIDTH = 190;
        private static final float BOX_HEIGHT = 86;
        private static final float BOX_TOP_OFFSET = 36;
        private static final float BIG_HEADER_HEIGHT = BOX_TOP_OFFSET + BOX_HEIGHT + 10;
        private static final float SLIM_HEADER_HEIGHT = 30;
        private static final float FOOTER_HEIGHT = 30;
        private static final float COL_GAP = 22;
        private static final float COL_WIDTH = (CONTENT_WIDTH - COL_GAP) / 2;
        private static final float LABEL_LINE = 11;
        private static final float VALUE_LINE = 13;
        private static final float ROW_GAP = 9;

        private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
        private static final PDFont REGULAR = PDType1Font.HELVETICA;
        private static final PDFont OBLIQUE = PDType1Font.HELVETICA_OBLIQUE;

        private static final Color BLACK = new Color(0, 0, 0);
        private static final Color GRAY_700 = new Color(70, 70, 70);
        private static final Color GRAY_500 = new Color(130, 130, 130);
        private static final Color GRAY_300 = new Color(205, 205, 205);
        private static final Color GRAY_100 = new Color(245, 245, 245);
        private static final Color WHITE = Color.WHITE;

        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;
        private String docTitle;
        private String docSurveyId;
        private final List<String[]> pendingFields = new ArrayList<>();

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
            if (docTitle != null) {
                drawSlimHeader();
            }
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN + FOOTER_HEIGHT) newPage();
        }

        void header(String title, String surveyId, String householdId, String surveyDate) throws IOException {
            this.docTitle = title;
            this.docSurveyId = surveyId;
            float topY = PAGE_HEIGHT - MARGIN;

            draw(BOLD, 19, MARGIN, topY - 18, BLACK, title);
            draw(REGULAR, 9.5f, MARGIN, topY - 33, GRAY_700, "Household Health Assessment Record");
            String generated = "Generated " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"));
            draw(REGULAR, 8.5f, MARGIN, topY - 48, GRAY_500, generated);

            float boxX = PAGE_WIDTH - MARGIN - BOX_WIDTH;
            float boxTop = topY - BOX_TOP_OFFSET;
            strokeRect(boxX, boxTop - BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT, BLACK, 1f);
            float rowH = BOX_HEIGHT / 3;
            String[][] rows = {
                    { "SURVEY ID", surveyId },
                    { "HOUSEHOLD ID", householdId },
                    { "DATE", surveyDate }
            };
            for (int i = 0; i < rows.length; i++) {
                float rowTop = boxTop - i * rowH;
                draw(BOLD, 7, boxX + 10, rowTop - 12, GRAY_700, rows[i][0]);
                draw(BOLD, i == 0 ? 13 : 10.5f, boxX + 10, rowTop - (i == 0 ? 25 : 24), BLACK, displayValue(rows[i][1]));
                if (i > 0) {
                    strokeLine(boxX, rowTop, boxX + BOX_WIDTH, rowTop, GRAY_300, 0.6f);
                }
            }

            float ruleY = topY - BIG_HEADER_HEIGHT;
            strokeLine(MARGIN, ruleY, PAGE_WIDTH - MARGIN, ruleY, BLACK, 1.6f);
            y = ruleY - 22;
        }

        private void drawSlimHeader() throws IOException {
            draw(BOLD, 10, MARGIN, PAGE_HEIGHT - 20, BLACK, docTitle);
            String meta = "Survey ID: " + docSurveyId;
            float metaWidth = REGULAR.getStringWidth(sanitize(meta)) / 1000 * 8.5f;
            draw(REGULAR, 8.5f, PAGE_WIDTH - MARGIN - metaWidth, PAGE_HEIGHT - 20, GRAY_700, meta);
            strokeLine(MARGIN, PAGE_HEIGHT - SLIM_HEADER_HEIGHT, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - SLIM_HEADER_HEIGHT,
                    BLACK, 1f);
            y = PAGE_HEIGHT - SLIM_HEADER_HEIGHT - 20;
        }

        void section(String value) throws IOException {
            flushGrid();
            ensureSpace(30);
            y -= 4;
            draw(BOLD, 12, MARGIN, y - 10, BLACK, value.toUpperCase());
            y -= 16;
            strokeLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, BLACK, 1f);
            y -= 14;
        }

        void field(String fieldLabel, String value) {
            pendingFields.add(new String[] { fieldLabel, value });
        }

        private void flushGrid() throws IOException {
            if (pendingFields.isEmpty()) return;
            for (int i = 0; i < pendingFields.size(); i += 2) {
                String[] left = pendingFields.get(i);
                String[] right = i + 1 < pendingFields.size() ? pendingFields.get(i + 1) : null;
                List<String> leftLines = wrap(displayValue(left[1]), REGULAR, 10.5f, COL_WIDTH);
                List<String> rightLines = right == null ? null : wrap(displayValue(right[1]), REGULAR, 10.5f, COL_WIDTH);
                float leftHeight = LABEL_LINE + leftLines.size() * VALUE_LINE;
                float rightHeight = rightLines == null ? 0 : LABEL_LINE + rightLines.size() * VALUE_LINE;
                float rowHeight = Math.max(leftHeight, rightHeight);
                ensureSpace(rowHeight + ROW_GAP);
                drawFieldCell(MARGIN, left[0], leftLines);
                if (right != null) drawFieldCell(MARGIN + COL_WIDTH + COL_GAP, right[0], rightLines);
                y -= rowHeight + ROW_GAP;
            }
            pendingFields.clear();
        }

        private void drawFieldCell(float x, String fieldLabel, List<String> valueLines) throws IOException {
            draw(BOLD, 8.5f, x, y, GRAY_700, fieldLabel.toUpperCase());
            boolean blank = valueLines.size() == 1 && "-".equals(valueLines.get(0));
            for (int i = 0; i < valueLines.size(); i++) {
                draw(REGULAR, 10.5f, x, y - LABEL_LINE - i * VALUE_LINE, blank ? GRAY_500 : BLACK, valueLines.get(i));
            }
        }

        private String displayValue(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }

        void healthTable(String itemColumnLabel, String[] keys, List<HealthItemEntry> items, SurveyMapper mapper)
                throws IOException {
            flushGrid();
            float itemW = CONTENT_WIDTH * 0.42f;
            float presentW = CONTENT_WIDTH * 0.13f;
            float detailW = CONTENT_WIDTH - itemW - presentW;
            float headerH = 20;
            ensureSpace(headerH);
            fillRect(MARGIN, y - headerH, CONTENT_WIDTH, headerH, BLACK);
            draw(BOLD, 9, MARGIN + 6, y - 14, WHITE, itemColumnLabel.toUpperCase());
            draw(BOLD, 9, MARGIN + itemW + 6, y - 14, WHITE, "PRESENT");
            draw(BOLD, 9, MARGIN + itemW + presentW + 6, y - 14, WHITE, "DETAILS");
            y -= headerH;

            int rowIndex = 0;
            for (String key : keys) {
                HealthItemEntry item = HealthItemEntry.findByKey(items, key);
                boolean present = item != null && Boolean.TRUE.equals(item.getPresent());
                String details = present ? visitDetails(item) : "";
                List<String> detailLines = details.isEmpty() ? List.of() : wrap(details, REGULAR, 8.5f, detailW - 12);
                float rowH = Math.max(1, detailLines.size()) * 12 + 8;
                ensureSpace(rowH);
                Color bg = rowIndex % 2 == 0 ? WHITE : GRAY_100;
                fillRect(MARGIN, y - rowH, CONTENT_WIDTH, rowH, bg);
                draw(REGULAR, 9.5f, MARGIN + 6, y - 14, BLACK, mapper.label(key));
                draw(BOLD, 9, MARGIN + itemW + 6, y - 14, present ? BLACK : GRAY_500, present ? "YES" : "No");
                if (detailLines.isEmpty()) {
                    draw(REGULAR, 8.5f, MARGIN + itemW + presentW + 6, y - 14, GRAY_500, "-");
                } else {
                    for (int i = 0; i < detailLines.size(); i++) {
                        draw(REGULAR, 8.5f, MARGIN + itemW + presentW + 6, y - 12 - i * 12, GRAY_700, detailLines.get(i));
                    }
                }
                strokeLine(MARGIN, y - rowH, PAGE_WIDTH - MARGIN, y - rowH, GRAY_300, 0.5f);
                y -= rowH;
                rowIndex++;
            }
            y -= 12;
        }

        void otherIssues(List<HealthItemEntry> items) throws IOException {
            flushGrid();
            boolean any = false;
            if (items != null) {
                for (HealthItemEntry item : items) {
                    if (item.getDescription() == null || item.getDescription().isBlank()) continue;
                    any = true;
                    String details = visitDetails(item);
                    List<String> descLines = wrap(item.getDescription(), REGULAR, 10, CONTENT_WIDTH - 16);
                    List<String> detailLines = details.isEmpty() ? List.of()
                            : wrap(details, OBLIQUE, 8.5f, CONTENT_WIDTH - 16);
                    float h = descLines.size() * VALUE_LINE + detailLines.size() * 11 + 10;
                    ensureSpace(h);
                    fillRect(MARGIN, y - 11, 4, 13, BLACK);
                    for (int i = 0; i < descLines.size(); i++) {
                        draw(REGULAR, 10, MARGIN + 12, y - 10 - i * VALUE_LINE, BLACK, descLines.get(i));
                    }
                    float dy = y - 10 - descLines.size() * VALUE_LINE;
                    for (String d : detailLines) {
                        draw(OBLIQUE, 8.5f, MARGIN + 12, dy, GRAY_700, d);
                        dy -= 11;
                    }
                    y -= h;
                }
                y -= 8;
            }
            if (!any) {
                field("Other Issues", "None reported");
                flushGrid();
            }
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

        private void draw(PDFont font, float size, float x, float py, Color color, String value) throws IOException {
            setColor(color, true);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, py);
            stream.showText(sanitize(value));
            stream.endText();
        }

        private void fillRect(float x, float py, float w, float h, Color color) throws IOException {
            setColor(color, true);
            stream.addRect(x, py, w, h);
            stream.fill();
        }

        private void strokeRect(float x, float py, float w, float h, Color color, float lineWidth) throws IOException {
            if (lineWidth <= 0) return;
            setColor(color, false);
            stream.setLineWidth(lineWidth);
            stream.addRect(x, py, w, h);
            stream.stroke();
        }

        private void strokeLine(float x1, float y1, float x2, float y2, Color color, float lineWidth) throws IOException {
            setColor(color, false);
            stream.setLineWidth(lineWidth);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private void setColor(Color color, boolean nonStroking) throws IOException {
            if (nonStroking) {
                stream.setNonStrokingColor(color);
            } else {
                stream.setStrokingColor(color);
            }
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
            flushGrid();
            if (stream != null) stream.close();
        }

        void paginate() throws IOException {
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage page = doc.getPage(i);
                try (PDPageContentStream fs = new PDPageContentStream(doc, page, AppendMode.APPEND, true)) {
                    fs.setStrokingColor(GRAY_300);
                    fs.setLineWidth(0.6f);
                    fs.moveTo(MARGIN, MARGIN - 6);
                    fs.lineTo(PAGE_WIDTH - MARGIN, MARGIN - 6);
                    fs.stroke();

                    fs.setNonStrokingColor(GRAY_500);
                    fs.beginText();
                    fs.setFont(REGULAR, 8);
                    fs.newLineAtOffset(MARGIN, MARGIN - 18);
                    fs.showText("Air Pollution & Community Health Survey");
                    fs.endText();

                    String pageLabel = "Page " + (i + 1) + " of " + total;
                    float w = REGULAR.getStringWidth(pageLabel) / 1000 * 8;
                    fs.beginText();
                    fs.setFont(REGULAR, 8);
                    fs.newLineAtOffset(PAGE_WIDTH - MARGIN - w, MARGIN - 18);
                    fs.showText(pageLabel);
                    fs.endText();
                }
            }
        }
    }
}
