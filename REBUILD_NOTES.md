# Rebuild Notes

This document explains what changed in this project and why, for anyone
picking up the repo after the fact. It covers: the local development
environment set up from scratch, a full rebuild of the survey form/data
model to match the field team's paper questionnaire, an expansion of the
CSV/Excel export to include every field, and a visual redesign of the
frontend.

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

## 3. Full data export

### Why

After the rebuild, the CSV/Excel export (`ExportService.java`) still only
produced the same curated ~18-column subset it always had — a
"Symptoms Summary" and "Conditions Summary" string, not the actual
per-item detail. That's not enough for real analysis: researchers need to
see, for every symptom and condition, whether it was present, whether the
respondent visited a hospital for it, the hospital name(s), IPD/OPD, missed
school/work, and days missed — exactly what the paper form captures.

### What changed

`ExportService.java` now builds its header row dynamically from
`SurveyCatalog.SYMPTOM_KEYS` (15) and `SurveyCatalog.CONDITION_KEYS` (7),
plus 5 fixed "Other Issue" slots, each expanded into its own set of
columns (Present / Visited Hospital / Hospital Name(s) / IPD / OPD /
Missed School/Work / Days Missed — 6 or 7 columns per item depending on
whether "Present" applies). Combined with the base demographic/survey
columns, this is a 224-column export where every field entered on the
form has its own column. `SurveyMapper.label()` was made public so
`ExportService` can reuse the same key-to-label formatting
(`DRY_COUGH` → `Dry Cough`) for column headers.

### Verification

Exported a CSV against the test record used for the earlier verification
pass and confirmed the expanded columns matched the database exactly
(e.g. `Asthma - Present/Visited Hospital/Hospital Name(s)/IPD/OPD/Missed
School-Work/Days Missed` → `Yes/Yes/A/Yes/Yes/Yes/2`, matching the raw
`conditions` JSON for that record).

## 4. Frontend visual redesign

### Why

The functional rebuild was done, but the visual design was generic and
inconsistent — colors and shadows were hard-coded ad hoc throughout
`styles.css` rather than following a shared system. The user asked for a
general visual refresh across the whole app, deferring to design judgment
on direction.

### What changed

- Introduced a CSS custom-property token system (`:root` in `styles.css`)
  for color, spacing, radius, and shadow, replacing the scattered
  hard-coded values. Every existing class name was kept as-is, so this is
  a restyle, not a restructure — no other component needed its
  `className` usage rewritten.
- `Login.jsx` restructured into a branded two-panel layout (gradient brand
  panel + form panel) instead of a single centered card.
- `StatCard.jsx` gained an optional `tone` prop (blue/teal/amber/rose/
  green) rendered as a colored top accent; `Dashboard.jsx` assigns a tone
  per metric so the stat grid isn't a wall of identical white boxes.
- Refined table styling (zebra striping, header treatment), button/badge
  states, and the `HealthItemTable` Y/N toggle buttons for better
  readability.

No backend, routing, or data-handling logic changed — this was a
presentation-layer-only pass. The login brand panel's "JWT-secured
accounts" line was later removed per feedback, leaving the offline and
role-based-access points.

## 5. Required-field navigation gating

### Why

The multi-section survey form marks some fields required with a `*`, but
the "Next" button never actually checked them — only the final Submit
validated the whole form and jumped back to the first incomplete section.
In practice a surveyor could click through every section without filling
anything required and only find out at the very end.

### What changed

`SurveyForm.jsx` now validates the *current* section's required fields
before advancing:

- "Next" runs `requiredMissingInSection` for the active section; if
  anything is missing it shows the error list and stays put instead of
  moving on.
- The numbered section tabs are locked past whichever section has been
  validated so far (`maxUnlockedSection` state) — you can always jump
  backward to review/edit earlier sections, but can't skip ahead of an
  incomplete one via the tabs either.
- Editing an existing record (`SurveyPage.jsx`, `mode === 'edit'`) passes
  a new `unlockAllSections` prop so the gate doesn't apply — an existing
  record is already complete, so the surveyor can jump straight to
  whichever section needs a fix rather than being forced through the
  wizard again.
- The final Submit-time full-form validation is unchanged, as a safety
  net.

## 6. Deployment prep (Render + Vercel)

Deploying to Render via the existing `render.yaml` blueprint surfaces a
bootstrap problem: with `SEED_DEMO_USERS=false` (the production default),
**no user accounts exist at all** on a fresh database, and there's no way
to create the first admin account other than through the admin-only Users
page — a chicken-and-egg problem.

`render.yaml` was temporarily changed to `SEED_DEMO_USERS: true` so the
first deploy seeds `admin`/`admin123`. The intended one-time bootstrap
sequence:

1. Deploy the blueprint (`SEED_DEMO_USERS=true`).
2. Log in as `admin`/`admin123`, immediately create a real admin account
   from the Users page, and disable both demo accounts.
3. Edit `render.yaml` back to `SEED_DEMO_USERS: false`, commit, and push
   — Render redeploys automatically on push to the connected branch.

Frontend (Vercel) and backend (Render) are deployed separately per the
README's existing "Deployment: Vercel + Render" section; `APP_FRONTEND_URL`
on the backend has to be set manually (`sync: false` in the blueprint)
once the Vercel URL is known, then `VITE_API_BASE_URL` on the frontend
once the Render URL is known — a two-way circle-back, also already
documented in the README.

### Deploy log

- Backend deployed at `https://air-pollution-survey-api.onrender.com`,
  frontend at `https://apchs.vercel.app`, both on free tiers.
- First backend deploy failed at startup with a Hibernate
  `Unable to determine Dialect without JDBC metadata` error. Root cause:
  Render's internal Postgres connection string omits the port (e.g.
  `postgresql://user:pass@dpg-xxxxx-a/dbname`, defaulting to 5432
  implicitly), but `DatabaseUrlNormalizer.java` blindly used
  `uri.getPort()` — which returns `-1` when a URI has no explicit port —
  producing an invalid `jdbc:postgresql://host:-1/db` URL. Fixed by
  defaulting to port 5432 when `uri.getPort() == -1`. This path was never
  exercised locally since local dev always supplies an explicit port.
- The bootstrap sequence above was completed: real admin account created,
  demo accounts disabled, `SEED_DEMO_USERS` reverted to `false` and
  redeployed.
- Both services are on free tiers for now, meaning the backend sleeps
  after 15 minutes of inactivity (cold start ~30-50s on the next
  request). Upgrading to a paid Render plan (so it stays always-on) is a
  deferred decision, not yet done.
