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
## Security Notes

- Do not commit real BioBook files, alumni exports, Railway credentials, Brevo keys, JWT secrets, database URLs, or production emails.
- Use backups before launch.
- Rotate any credential that is accidentally committed or pasted into a public issue/log.
