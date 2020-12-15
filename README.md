# familie-tilbake
Applikasjon for tilbakekreving av barnetrygd og enslig forsørger

## Bygging
Bygging gjøres med `mvn clean install`.

## Kjøring lokalt
For å kjøre opp appen lokalt kan en kjøre `DevLauncher` med Spring-profilen `dev` satt. Dette kan feks gjøres ved å sette
`-Dspring.profiles.active=dev` under Edit Configurations -> VM Options.
Appen tilgjengeliggjøres da på `localhost:8030`.

### Database
Dersom man vil kjøre med postgres, kan man bytte til Spring-profilen `postgres`. Da må man sette opp postgres-databasen, dette gjøres slik:
```
docker run --name familie-tilbake-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
winpty docker exec -it <container_id> bash(fra git-bash windows)
psql -U postgres
CREATE DATABASE "familie-tilbake";
\l (til å verifisere om databasen er opprettet)
```

### Autentisering
Dersom man vil gjøre autentiserte kall mot andre tjenester, må man sette opp følgende miljø-variabler:
* Client secret
* Client id
* Scope for den aktuelle tjenesten

Alle disse variablene finnes i applikasjonens mappe for preprod-fss på vault.
Variablene legges inn under DevLauncher -> Edit Configurations -> Environment Variables.

## Produksjonssetting
Master-branchen blir automatisk bygget ved merge og deployet til prod.

## Kontaktinformasjon
For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie-tilbakekreving på slack. Ellers kan man opprette et issue her på github.
