# Wharton EMBA Alumni Backend

Spring Boot Java API for the Wharton EMBA Alumni Portal. This replaces the original Next.js backend recommendation with a dedicated Java service.

## Run locally

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api` and allows CORS from the Vite frontend at `http://localhost:5173`.

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
