# Wharton EMBA Alumni Backend

Spring Boot Java API for the Wharton EMBA Alumni Portal. This replaces the original Next.js backend recommendation with a dedicated Java service.

## Run locally

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api` and allows CORS from the Vite frontend at `http://localhost:5173`.
Without database environment variables, the app uses an in-memory H2 database for local development.

To load demo data on startup locally:

```bash
SEED_DATA_ENABLED=true mvn spring-boot:run
```

## Seeded data

All seeded users use password `password`.

- `admin@wharton.example`
- `maya.chen@wharton.example`
- `diego.ramirez@wharton.example`
- `sarah.okafor@wharton.example`
- `jonathan.lee@wharton.example`
- `priya.menon@wharton.example`

## Reseed demo data

With the backend running:

```bash
./scripts/seed-data.sh
```

Use a different API URL if needed:

```bash
API_BASE_URL=http://localhost:8080/api ./scripts/seed-data.sh
```

## Railway deployment

This backend is ready to deploy on Railway with a Railway Postgres service.

1. Create a Railway project from this GitHub repository.
2. Add a Railway Postgres database service.
3. Set the backend service variables below.
4. Deploy the backend service.
5. Set the frontend `VITE_API_BASE_URL` to `https://<backend-domain>/api`.

Required Railway variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
APP_CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>
SEED_DATA_ENABLED=false
```

Optional variables:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

Use `SEED_DATA_ENABLED=true` only for throwaway demo environments. It deletes and recreates seeded alumni/events data.

Railway uses the included `Dockerfile` and `railway.json`. Health checks use `/actuator/health`.

## Production notes

- BioBook claim data is intentionally not committed. Load it into Postgres through a controlled import path.
- The current auth response still returns demo tokens. Replace this with JWT/session auth before handling real users.
- For production, restrict `APP_CORS_ALLOWED_ORIGINS` to the deployed frontend domain only.
- Enable Railway Postgres backups/PITR before launch.
