# AGENTS.md

Veiledning for AI-kodingsagenter som arbeider i dette repoet.

## Prosjektoversikt

`familie-tilbake` er en Spring Boot-applikasjon skrevet i Kotlin for håndtering av tilbakekreving av barnetrygd, kontantstøtte og enslig forsørger. Applikasjonen er en del av NAV's tilbakekrevingsdomene og kjører på NAIS-plattformen.

## Teknisk stack

- **Språk:** Kotlin (JVM 25)
- **Rammeverk:** Spring Boot 4
- **Database:** PostgreSQL med Flyway-migrasjoner og Spring Data JDBC
- **Meldingskø:** IBM MQ / JMS og Apache Kafka(kun testing)
- **HTTP-klient:** Ktor
- **Bygg:** Gradle (Kotlin DSL)

## Modulstruktur

```
familie-tilbake/           # Rotprosjekt – Spring Boot-applikasjonen
├── felles/                # Delte utilities og hjelpeklasser
├── integrasjoner/         # Integrasjoner mot eksterne tjenester
├── kontrakter-ekstern/    # Kontrakter fra eksterne systemer
├── kontrakter-ekstern-v2/ # v2-varianter av eksterne kontrakter
├── kontrakter-fagsystem/  # Kontrakter mot fagsystemer
├── kontrakter-felles/     # Felles kontrakter
├── kontrakter-frontend/
│   ├── api/               # API-kontrakter mot frontend
│   └── dtoer/             # DTO-klasser for frontend
├── kontrakter-intern/     # Interne kontrakter
├── modell/                # Domenemodell
├── pdf/                   # PDF-generering (gammel kode, skal ikke benyttes)
└── testdata/              # Testhjelpere (kun test-scope)
```

## Bygge- og testkjøring

### Vanlige kommandoer

```bash
# Full bygg inkludert tester
./gradlew build

# Bygg uten tester (raskere)
./gradlew build -xtest

# Kjør alle tester
./gradlew test

# Kjør tester for ett submodul
./gradlew :modell:test
```

### Linting (ktlint)

```bash
# Sjekk kodestil
./gradlew ktlintCheck

# Fiks kodestil automatisk
./gradlew ktlintFormat
```

Ktlint kjøres på alle pull requests i CI. **Alltid kjør `ktlintCheck` eller `ktlintFormat` før du committer endringer.**

## Kodestilregler

- Følg standard Kotlin-konvensjoner
- Linjegrense er satt til `off` (ingen hard grense), men hold linjer lesbare
- Bruk Kotest assertions (`shouldBe`, `shouldNotBe`, osv.) i tester fremfor JUnit assertions
- Bruk kun kode fra `no.nav.tilbakekreving`-pakken for inspirasjon
- Foretrekk å skrive testkode i `modell`-modulen
- Logikk skal implementeres i `modell`-modulen
- rot-modulen skal kun inneholde kode som kobler modellen mot eksterne systemer

## Testkonvensjoner

- Unngå mocking, foretrekk alltid stubs av interface
- Unngå å skrive en integrasjonstest med mindre det er nødvendig
- **Enhetstest:** JUnit Jupiter + Kotest assertions
- **Integrasjonstest:** Testcontainers (PostgreSQL, ActiveMQ) + WireMock
- Integrasjonstester arver fra `OppslagSpringRunnerTest` som setter opp Spring-kontekst med testcontainers
- Testdata og builders finnes i `:testdata`-modulen

## Databasemigrasjoner

Flyway-migrasjoner ligger under `src/main/resources/db/migration/`. Nye migrasjoner skal:
- Navngis `V<neste_versjon>__beskrivelse.sql`
- Være bakoverkompatible der det er mulig
- Aldri endre eksisterende migrasjonsfiler

## Viktige konvensjoner

- Alle nye REST endepunkter skal komme fra eksterne kontrakter generert av typespec i tilbakekreving-kontrakter
- Domenelogikk skal kun ligge i `modell`-modulen
- Spring boot skal abstraheres bort så tidlig som mulig
- All logging skal følge NAV's strukturerte loggformat (logstash-logback-encoder)
- Logging skal aldri bruke kotlin string templating, det skal håndteres av logback 

## CI/CD

- **Pull requests:** Kjører ktlint, bygg, JUnit-tester og e2e-tester
- **Merge til main:** Bygger og deployer automatisk til produksjon
- Draft pull requests kjører ikke bygg eller tester

## Avhengigheter mot andre repoer

- [familie-tilbake-frontend](https://github.com/navikt/familie-tilbake-frontend) – frontend
- [tilbakekreving-kontrakter](https://github.com/navikt/tilbakekreving-kontrakter) – kontrakter for REST-endepunkt og Kafka-events
