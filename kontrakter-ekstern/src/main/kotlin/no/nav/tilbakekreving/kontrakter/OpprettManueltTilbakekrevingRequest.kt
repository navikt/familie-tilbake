package no.nav.tilbakekreving.kontrakter

import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

// Denne brukes fagsystemet for å kalle /api/behandling/manuelt/task/v1 tjeneste i familie-tilbake
data class OpprettManueltTilbakekrevingRequest(
    val eksternFagsakId: String,
    val ytelsestype: YtelsestypeDTO,
    // Fagsystemreferanse til behandlingen, må være samme id som brukes mot datavarehus og økonomi
    val eksternId: String,
)

// familie-tilbake bruker denne dto-en til å sende respons på ../kanBehandlingOpprettesManuelt/v1 tjeneste
data class KanBehandlingOpprettesManueltRespons(
    val kanBehandlingOpprettes: Boolean,
    val melding: String,
    val kravgrunnlagsreferanse: String? = null,
)
