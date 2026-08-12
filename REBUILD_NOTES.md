# Rebuild Notes

This document explains what changed in this project and why, for anyone
picking up the repo after the fact. It covers two things: the local
development environment that was set up from scratch, and a full rebuild of
the survey form/data model to match the field team's paper questionnaire.

## 1. Local development environment

The project previously had no working local dev setup on this machine.
The following was set up:

- **PostgreSQL 18** was already installed as a Windows service
  (`postgresql-x64-18`). The `air_pollution_survey` database existed but was
  empty; `backend/.env` and `frontend/.env` were created (gitignored) with
  working local credentials so the backend and frontend can run locally.
- **Node.js, Git, and Apache Maven** were not installed anywhere on the
  machine. All three were installed:
  - Node.js: standard installer.
  - Git: standard installer (git-scm.com).
  - Maven: no Windows installer exists for it, so the binary zip was
    extracted to `%USERPROFILE%\tools\maven`, with `JAVA_HOME` and `PATH`
    set at the user level (writing to `C:\Program Files` required
    elevation that wasn't available).
- Confirmed the full stack runs together locally: backend on
  `http://localhost:8080`, frontend on `http://localhost:5173`, login
  working against the two demo accounts seeded via `SEED_DEMO_USERS=true`
  (`admin`/`admin123`, `surveyor`/`survey123` — **local/dev only, never use
  in production**, see main `README.md`).

## 2. Survey form rebuild to match the paper form

### Why

The field team uses a physical two-page paper form ("AIR POLLUTION –
COMMUNITY HEALTH SURVEY") for household interviews. The app's original data
model predates that paper form and didn't match it: it asked different
demographic questions, tracked symptoms on a severity scale
(Never/Sometimes/Often/Daily) instead of the paper's Y/N + hospital-visit
detail, and computed an automatic risk score from exposure factors
(ventilation, dust, road proximity, etc.) that the paper form doesn't even
collect. The app was rebuilt field-by-field to match what the paper form
actually asks, so the digital tool mirrors what surveyors are trained to do
and the two stay in sync going forward.

### What changed

**Data model**, per the paper's sections:

- Survey info (date, surveyor, consent, study area, GPS, grid ID, distance
  to highway/factory)
- Demographics (age, duration of stay, gender, tobacco use, alcohol,
  ethnicity, education, occupation)
- Cooking (fuel type, indoor/outdoor wood-coal cooking location)
- Children & vaccination (children present/count, birthplace, vaccination,
  respondent vaccination, MHIS smart card)
- **Conditions** — a fixed 7-item catalog (Asthma, Inhaler Use,
  Tuberculosis, Heart Problems, Diabetes, High BP, Cancer), each item
  carrying: present, visited hospital, hospital name(s), IPD, OPD, missed
  school/work, days missed.
- **Symptoms** — the same per-item shape, over a fixed 15-item catalog
  matching the paper (Headache, Eye Irritation, Rhinitis, Sneezing,
  Sinusitis, Sore Throat, Cold, Fever, Dry Cough, Wet Cough, Wheezing,
  Breathlessness, Chest Discomfort, Sleep Disturbance, Skin Irritation),
  plus a standalone fever-duration field.
- **Other issues** — up to 5 free-text rows with the same hospital/IPD/OPD/
  missed-day detail.

The three repeating item lists (`symptoms`, `conditions`, `otherIssues`) are
stored as **JSONB columns** on `survey_records` (via Hibernate's native
`@JdbcTypeCode(SqlTypes.JSON)` support) rather than as ~189 individual flat
columns — the previous approach would have taken for a data shape this
repetitive.

**Dropped entirely**: the old automatic risk-scoring system
(`exposureRiskScore`, `symptomScore`, `vulnerabilityScore`,
`totalRiskScore`, `riskLevel`, and the age-group derivation that fed it).
The paper form no longer collects enough exposure detail to support that
formula, and the new symptom shape (Y/N + hospital detail) isn't a severity
scale, so there was nothing sound left to compute a score from. Everything
downstream — the dashboard's risk chart, the `riskLevel` filter/export
column, risk badges in the records list — was removed with it.

**Kept**: consent (`consentObtained`) is still required and hard-blocked on
the backend, even though it isn't a line item on the paper form — it's a
compliance requirement, not a data field.

### Backend files touched

- New: `entity/HealthItemEntry.java` (the shared per-item shape),
  `service/SurveyCatalog.java` (the fixed symptom/condition key lists)
- Rewritten: `entity/SurveyRecord.java`, `dto/SurveyPayload.java`,
  `dto/SurveyResponse.java`, `service/SurveyMapper.java`,
  `service/ExportService.java`, `service/DashboardService.java`,
  `dto/DashboardSummaryResponse.java`
- Deleted: `service/RiskScoringService.java`
- Edited: `service/SurveyService.java` (dropped risk-scoring calls and the
  old district/block/village/riskLevel/visitedHospital filters; the
  `symptom` filter is now an in-memory scan over the JSON column instead of
  a SQL predicate, since JPA Criteria can't easily query into JSON)

### Frontend files touched

- New: `components/HealthItemTable.jsx` (renders the paper-style repeating
  table for symptoms/conditions/other-issues, in both editable and
  read-only modes)
- Rewritten: `utils/surveyConfig.js`, `components/SurveyForm.jsx`,
  `pages/SurveyDetail.jsx`, `pages/Records.jsx`, `pages/Dashboard.jsx`
- Deleted: `utils/risk.js`
- Edited: `components/ChartPanel.jsx` (removed risk-level and stale fuel
  color entries), `pages/OfflineQueue.jsx` (its record-summary column
  referenced a field, `respondentName`, that no longer exists), `styles.css`
  (removed now-dead `.risk-preview`/`.risk-pill`/`.risk-badge` rules, added
  styles for the new health-item table)

### Database

The local `survey_records` table was empty (freshly created earlier the
same session), so rather than let Hibernate's `ddl-auto=update` accumulate
orphaned old columns next to the new ones, the table was dropped and
Hibernate rebuilt it clean from the new entity on the next boot.

### Verification performed

- Backend compiled and booted cleanly against the new entity; confirmed via
  `psql` that `symptoms`/`conditions`/`otherIssues` came out as real
  `jsonb` columns.
- Submitted a full test survey via the API covering every section
  (including populated symptom/condition/other-issue arrays) and confirmed
  it round-tripped correctly through `GET /api/surveys/{id}`.
- Confirmed the survey list, the dashboard summary (respiratory-symptom
  count, hospital-visit count, average missed days, common-symptoms
  breakdown), the reworked `symptom` filter, and CSV export all correctly
  read from the new JSON-backed model.
- Grepped both codebases for leftover references to every removed
  field/class — none found.
- Frontend visual verification against the paper form photos was left for
  manual check in the browser, since it isn't something that can be
  confirmed via API calls alone.
