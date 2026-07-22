# familie-tilbake

Backend for tilbakekreving av barnetrygd, kontantstøtte og enslig forsørger.

## Kom i gang

### Krav

- JDK 25
- Docker
- Personal access token (classic) med tilgang til å lese og skrive pakker, og som er autentisert mot NAV
- [`gcloud` CLI](https://cloud.google.com/sdk/docs/install) og tilgang via Naisdevice

### Bygg

```bash
./gradlew build
```

Bygg uten tester:

```bash
./gradlew build -x test
```

> Merk: Bygget laster ned en BigQuery JDBC-driver automatisk. Sørg for internettilgang første gang.

## Kjøring lokalt

Appen eksponeres på `http://localhost:8030`.

### Alternativ 1 – med Testcontainers (enklest)

Bruker `LauncherLocal.kt` og starter en PostgreSQL-container automatisk via Testcontainers. Krever Docker.

1. For å starte applikasjonen, må man være logget inn hos gcloud og naisdevice. Dette gjøres med:
   ```bash
   nais auth login
   ```
2. Start `LauncherLocal` fra IntelliJ (kjør `main`-funksjonen i `src/test/kotlin/.../LauncherLocal.kt`)

### Alternativ 2 – med lokal PostgreSQL

Bruker `LauncherLocalPostgres.kt` og kobler til en Postgres-instans på `localhost:5432`.

1. Start databasen:
   ```bash
   docker run --name familie-tilbake-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
   ```
   Hvis containeren allerede finnes:
   ```bash
   docker start familie-tilbake-postgres
   ```
2. Opprett databasen:
   ```bash
   docker exec -it familie-tilbake-postgres psql -U postgres -c 'CREATE DATABASE "familie-tilbake";'
   ```
3. Logg inn på gcloud:
   ```bash
   gcloud auth login
   ```
4. Start `LauncherLocalPostgres` fra IntelliJ

### Dummy-behandling

Begge launcherne oppretter en dummy-behandling ved oppstart. Linken skrives til konsollen:

```
http://localhost:4000/fagsystem/EF/fagsak/1234567/behandling/{generertBehandlingId}
```

Søk på `dummy-behandling` i konsolloutput for å finne den.

### Secrets

Secrets hentes automatisk fra GCP. Krever at du er logget inn med `gcloud auth login` og er på Naisdevice.

## Relaterte repoer

For å teste hele flyten lokalt trenger du typisk:

| Repo | Formål |
|------|--------|
| [familie-tilbake-frontend](https://github.com/navikt/familie-tilbake-frontend) | Frontend |
| [familie-tilbake-e2e](https://github.com/navikt/familie-tilbake-e2e) | E2E-tester og opprettelse av behandlinger |
| [tilbakekreving-kontrakter](https://github.com/navikt/tilbakekreving-kontrakter) | REST- og Kafka-kontrakter |

## Kodekvalitet

# Sjekk kodestil
./gradlew ktlintCheck
```

## CI/CD

Pull requests kjører ktlint, bygg og tester automatisk. Merge til `main` deployer automatisk til produksjon.

## Kontaktinformasjon
For NAV-interne kan henvendelser om applikasjonen rettes til #baks-dev på slack.
Ellers kan man opprette et issue her på github.

# Generering av dokumentasjon
https://confluence.adeo.no/display/TFA/Generert+dokumentasjon

[Filer for generering av dokumentasjon](/src/test/kotlin/no/nav/familie/tilbake/dokumentasjonsgenerator)

# Dokumentasjon - flytdiagrammer
https://confluence.adeo.no/display/TFA/Motta+kravgrunnlag+flyt
