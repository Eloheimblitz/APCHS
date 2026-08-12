# Air Pollution - Community Health Survey

Production-ready full-stack household data collection system for an air pollution and community health survey.

The application supports JWT login, role-based survey access, PostgreSQL storage, dashboard summaries, survey CRUD, filtered record search, GPS capture, and CSV/Excel export. The survey form mirrors the field team's paper "AIR POLLUTION – COMMUNITY HEALTH SURVEY" instrument, including per-symptom/condition detail (hospital visit, IPD/OPD, missed school/work, days missed). See [REBUILD_NOTES.md](REBUILD_NOTES.md) for the history of that rebuild.
Admins can also create surveyor accounts, reset passwords, change roles, and disable accounts. Every signed-in user can change their own password from the Account page.
The frontend includes offline capture for new surveys: submissions made without a server connection are stored in IndexedDB and shown on the Pending Sync page until they are synced.

## Tech Stack

- Frontend: React 18, Vite, React Router, Axios, Recharts, responsive CSS
- Backend: Spring Boot 3, Java 21, Spring Web, Spring Data JPA, Spring Security JWT, Maven
- Database: PostgreSQL
- Export: OpenCSV and Apache POI
- Deployment: Vercel frontend, Render or Railway backend and PostgreSQL

## Folder Structure

```text
air-pollution-health-survey/
├── frontend/
│   ├── src/api/
│   ├── src/components/
│   ├── src/layouts/
│   ├── src/pages/
│   ├── src/utils/
│   ├── src/App.jsx
│   └── src/main.jsx
├── backend/
│   ├── src/main/java/com/airpollution/survey/
│   ├── src/main/resources/application.properties
│   ├── Dockerfile
│   └── pom.xml
├── deployment-notes.md
└── README.md
```

## Local Setup

### Backend

1. Install Java 21 and PostgreSQL.
2. Create the database:

```sql
CREATE DATABASE air_pollution_survey;
```

3. Configure environment variables or copy `backend/.env.example`.

Required values:

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/air_pollution_survey
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=replace_with_a_long_random_secret_at_least_32_characters
APP_FRONTEND_URL=http://localhost:5173
```

4. Start the backend:

```bash
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api`.

### Frontend

1. Install Node.js 18+.
2. Create `frontend/.env`:

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

3. Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173`.

## Demo Login

Demo users are optional and should only be used for local testing:

- Admin: `admin` / `admin123`
- Surveyor: `surveyor` / `survey123`

Passwords are seeded through `CommandLineRunner` and stored with BCrypt hashes only when `SEED_DEMO_USERS=true`. For real field work and production deployments, keep `SEED_DEMO_USERS=false`, create real admin/surveyor accounts from the Users page, and do not use the demo credentials. The production login screen does not display demo credentials.

## API Endpoints

Authentication:

- `POST /api/auth/login`

User management, admin only:

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{id}`

Account:

- `PUT /api/users/me/password`

Survey records:

- `POST /api/surveys`
- `GET /api/surveys`
- `GET /api/surveys/{id}`
- `PUT /api/surveys/{id}`
- `DELETE /api/surveys/{id}`

Filters supported by list and export:

```text
studyArea
fromDate
toDate
cookingFuel
symptom
```

`symptom` matches against the fixed symptom catalog (see `SurveyCatalog.SYMPTOM_KEYS`
in the backend, e.g. `DRY_COUGH`, `WHEEZING`) and returns records where that symptom
is marked present.

Dashboard:

- `GET /api/dashboard/summary`

Export:

- `GET /api/export/surveys.csv`
- `GET /api/export/surveys.xlsx`

Example:

```text
/api/export/surveys.xlsx?studyArea=BYRNIHAT&cookingFuel=WOOD
```

## Roles

Admin can view dashboard, add surveys, view all records, edit, delete, filter, and export.
Admin can also manage user accounts.

Surveyor can add surveys, view submitted records created by their login, and edit those records.

## Data Quality And Privacy

- Use standardized enum-like values for filters and exports.
- Required fields are enforced on both frontend and backend for survey/demographic core fields and conditional "OTHER" answers.
- Do not expose database credentials in the frontend.
- Do not commit real `.env` files.
- The app displays a privacy note on login.
- Backend validation rejects surveys without consent.

## Deployment: Vercel + Render

### Frontend on Vercel

- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`
- Environment variable:

```text
VITE_API_BASE_URL=https://YOUR_RENDER_BACKEND_URL/api
```

### Backend on Render

- Root directory: `backend`
- Build command: `mvn clean package -DskipTests`
- Start command: `java -jar target/*.jar`
- Environment variables:

```text
DATABASE_URL=jdbc:postgresql://HOST:PORT/DB_NAME
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
JWT_SECRET=generate_a_long_secure_secret
APP_FRONTEND_URL=https://YOUR_VERCEL_FRONTEND_URL
```

Create a Render PostgreSQL database, copy its host, port, database, username, and password into the backend environment variables, then redeploy the backend.

## Deployment: Vercel + Railway

Frontend on Vercel is the same as above.

Backend on Railway:

1. Deploy the `backend` folder from GitHub.
2. Add a Railway PostgreSQL database.
3. Add:

```text
DATABASE_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
APP_FRONTEND_URL
```

Use a JDBC-form PostgreSQL URL for `DATABASE_URL`, for example:

```text
jdbc:postgresql://HOST:PORT/DB_NAME
```

## CORS Production Notes

Set `APP_FRONTEND_URL` exactly to the deployed frontend origin, such as:

```text
https://your-project.vercel.app
```

After the backend URL changes, update `VITE_API_BASE_URL` in Vercel and redeploy the frontend.

## Production Deployment Files

- `render.yaml`: Render blueprint for backend and PostgreSQL.
- `backend/Dockerfile`: backend container image.
- `backend/railway.json`: Railway backend Docker configuration.
- `frontend/vercel.json`: Vercel SPA rewrite configuration.
- `backend/.env.production.example`: backend production environment template.
- `frontend/.env.production.example`: frontend production environment template.
- `PRODUCTION-CHECKLIST.md`: step-by-step production checklist.

Production notes:

- Backend health check endpoint: `GET /api/health`
- Hosted PostgreSQL URLs can be `postgresql://...` or `jdbc:postgresql://...`.
- Set `SEED_DEMO_USERS=false` for production after you have a real admin account plan.

## Redeploying

1. Push changes to GitHub.
2. Render or Railway redeploys the backend from the backend folder.
3. Vercel redeploys the frontend from the frontend folder.
4. If backend URL changes, update `VITE_API_BASE_URL`.
5. If frontend URL changes, update backend `APP_FRONTEND_URL`.

## Offline/PWA Note

The frontend registers a service worker and stores new offline survey submissions in IndexedDB. Surveyors should log in once while online before fieldwork. If the backend is unreachable while adding a new survey, the app saves it in Pending Sync. When the connection returns, users can press Sync now or let the app attempt sync when the browser reports it is online.

Current offline scope:

- New survey submissions are queued offline.
- Existing record edits are online-only to avoid conflicting updates.
- Login must happen online at least once so the JWT token is available.
