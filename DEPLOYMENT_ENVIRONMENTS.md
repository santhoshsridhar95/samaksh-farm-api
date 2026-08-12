# Deployment Environments

Run separate API deployments and separate Postgres databases for development and production.

## Profiles

| Environment | Spring Profile | Database | UI |
| --- | --- | --- | --- |
| Development | `dev` | `samaksh_farms_dev` or a dev cloud DB | Dev frontend URL |
| Production | `prod` | Dedicated production DB | Production frontend URL |

## Required Production Variables

`prod` intentionally fails fast if these are missing:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `EMAIL_VERIFICATION_BASE_URL`

Set mail and Google variables only when those features are enabled.

## Local Development

Copy `.env.dev.example` to an untracked `.env.dev` or set the variables in your shell.

```bash
set SPRING_PROFILES_ACTIVE=dev
.\mvnw.cmd spring-boot:run
```

## Production Deployment

Use `.env.prod.example` as the checklist for your hosting platform. Do not commit the filled production file.

The production profile uses `spring.jpa.hibernate.ddl-auto=validate` by default. That prevents accidental schema changes from application startup. If you intentionally need automatic schema changes during an early deployment, set `JPA_DDL_AUTO=update` temporarily and switch it back after the schema is ready.
