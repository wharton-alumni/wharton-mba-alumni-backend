# Wharton EMBA Alumni Backend

Spring Boot API for the Wharton EMBA Alumni Portal.

## Local Development

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api`. Without database environment variables, the app uses an in-memory H2 database.

Optional local-only seed data can be enabled for throwaway testing:

```bash
SEED_DATA_ENABLED=true mvn spring-boot:run
```

Seed fixtures are synthetic and must not contain real alumni, BioBook, Railway, or email-provider data.

## Production Configuration

Required variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<database-password>
JWT_SECRET=<long-random-secret>
APP_CORS_ALLOWED_ORIGINS=https://whartonemba.com,https://www.whartonemba.com
SEED_DATA_ENABLED=false
BIOBOOK_SEED_ENABLED=false
```

Email variables:

```text
BREVO_API_KEY=<brevo-api-key>
EMAIL_SENDER_NAME=Wharton EMBA Alumni Portal
EMAIL_SENDER_EMAIL=<verified-sender-email>
EMAIL_DEMO_RECIPIENTS=<optional-comma-separated-test-recipients>
```

Headshot storage variables:

```text
HEADSHOT_BUCKET_NAME=<bucket-name>
HEADSHOT_BUCKET_ENDPOINT=<s3-compatible-endpoint>
HEADSHOT_BUCKET_REGION=<region>
HEADSHOT_BUCKET_ACCESS_KEY_ID=<access-key>
HEADSHOT_BUCKET_SECRET_ACCESS_KEY=<secret-key>
```

External event refresh variables:

```text
EXTERNAL_EVENTS_ENABLED=true
EXTERNAL_EVENTS_REFRESH_HOURS=24
EXTERNAL_EVENTS_STARTUP_DELAY_MS=1000
EXTERNAL_EVENTS_REFRESH_CHECK_MS=3600000
```

`GET /api/events` serves events from Postgres. External Wharton event scraping is a background refresh and should not block user page loads.

## Security Notes

- Do not commit real BioBook files, alumni exports, Railway credentials, Brevo keys, JWT secrets, database URLs, or production emails.
- Keep `SEED_DATA_ENABLED=false` and `BIOBOOK_SEED_ENABLED=false` in production.
- Use Railway/Postgres backups before launch.
- Rotate any credential that is accidentally committed or pasted into a public issue/log.
