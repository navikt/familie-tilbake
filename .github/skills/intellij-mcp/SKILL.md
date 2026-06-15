---
name: intellij-mcp
description: Uses the IntelliJ MCP tools for various programming tasks. Use when the IntelliJ MCP is available and you are building, running tests, searching or refactoring.
allowed-tools:
  - get_run_configurations
  - execute_run_configuration
  - search_symbol
  - search_text
  - search_regex
  - rename_refactoring
  - get_file_problems
  - build_project
---

# IntelliJ MCP-verktøy

Når IntelliJ MCP er tilgjengelig, foretrekk disse verktøyene fremfor kommandolinje:

- **Validering av endringer:** Bruk `build_project` (kompilerer kun endrede filer) for rask
  tilbakemelding på kompileringsfeil i stedet for full `./gradlew build`.
- **Inspeksjoner:** Kjør `get_file_problems` på en fil for å fange feil og advarsler før bygg.
- **Refaktorering:** Bruk `rename_refactoring` for trygg, prosjektomfattende omdøping av symboler.
- **Søk/navigasjon:** Foretrekk `search_symbol`, `search_text` og `search_regex` (indeksert)
  fremfor `grep` på kommandolinjen.
- **Kjøre tester:** Bruk `get_run_configurations` + `execute_run_configuration`. Bruk kun
  kjørekonfigurasjoner som er lagret i prosjektet (`.run/`):
  `Tests in 'familie-tilbake.modell'`, `Tests in 'familie-tilbake.test'` og
  `Tests in 'familie-tilbake.pdf.test'`. Personlige konfigurasjoner i `.idea/workspace.xml`. Kjør tester relatert til modulene som endres, men kjør også alltid testene i `familie-tilbake.test`.
