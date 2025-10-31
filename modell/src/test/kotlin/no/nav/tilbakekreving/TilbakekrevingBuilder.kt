package no.nav.tilbakekreving

import java.time.LocalDateTime
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import java.util.UUID

private val bigQueryService = BigQueryServiceStub()

fun opprettTilbakekreving(
    oppsamler: BehovObservatørOppsamler,
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
) = Tilbakekreving
    .opprett(
        id = UUID.randomUUID().toString(),
        behovObservatør = oppsamler,
        opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
        bigQueryService = bigQueryService,
        endringObservatør = EndringObservatørOppsamler(),
    )

fun tilbakekrevingTilBehandling(
    oppsamler: BehovObservatørOppsamler,
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
): Tilbakekreving {
    val tilbakekreving = opprettTilbakekreving(oppsamler, opprettTilbakekrevingHendelse)
    tilbakekreving.apply {
        håndter(kravgrunnlag())
        håndter(fagsysteminfoHendelse())
        håndter(brukerinfoHendelse())
        håndter(VarselbrevSendtHendelse(varselbrevId = tilbakekreving.brevHistorikk.nåværende().entry.id, journalpostId = "1234", sendtTid = LocalDateTime.now()))
    }
    return tilbakekreving
}
