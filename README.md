# familie-tilbake
Applikasjon for tilbakekreving av barnetrygd, kontantstøtte og enslig forsørger

## Bygging
Bygging gjøres med `mvn verify`.

## Kjøring lokalt
For å kjøre opp appen lokalt kan en kjøre `LauncherLocalPostgres.kt`, eller `LauncherLocal.kt` om du ikke vil kjøre opp 
databasen selv. Begge krever at du har logget deg på gcloud `gcloud auth login` og at du er på Naisdevice.  
Appen tilgjengeliggjøres da på `localhost:8030`.

Det opprettes en dummy behandling ved oppstart, link til behandling skrives i console og vil se slik ut: http://localhost:4000/fagsystem/EF/fagsak/1234567/behandling/{generertBehandlingId}
Søk på `dummy-behandling` for å finne linken i console.

### Lokale avhengigheter
For å teste tilbakekreving lokalt må du mest sannsynlig også sette opp disse repoene
* [Familie-tilbake-frontend](https://github.com/navikt/familie-tilbake-frontend)
* [Familie-tilbake-e2e](https://github.com/navikt/familie-tilbake-e2e) for å sette opp behandlinger

### Database
Dersom man vil kjøre med postgres må man sette opp postgres-databasen, dette gjøres slik:
```
docker run --name familie-tilbake-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
( For mac ) docker exec -it <container_id> bash
( For windows ) winpty docker exec -it <container_id> bash(fra git-bash windows)
psql -U postgres
CREATE DATABASE "familie-tilbake";
\l (til å verifisere om databasen er opprettet)
```

Når `docker run --name familie-tilbake-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres` gir feilmelding om at det allerede finnes en container med id `_ID_` må du kjøre:
`docker start _ID_`

### Secrets
Secrets hentes automatisk. Dette krever at du har logget deg på gcloud `gcloud auth login` og at du er på Naisdevice.

## Produksjonssetting
Master-branchen blir automatisk bygget ved merge og deployet til prod.

## Kontaktinformasjon
For NAV-interne kan henvendelser om applikasjonen rettes til #baks-dev på slack.
Ellers kan man opprette et issue her på github.

# Generering av dokumentasjon
https://confluence.adeo.no/display/TFA/Generert+dokumentasjon

[Filer for generering av dokumentasjon](/src/test/kotlin/no/nav/familie/tilbake/dokumentasjonsgenerator)

# Dokumentasjon - flytdiagrammer
https://confluence.adeo.no/display/TFA/Motta+kravgrunnlag+flyt
