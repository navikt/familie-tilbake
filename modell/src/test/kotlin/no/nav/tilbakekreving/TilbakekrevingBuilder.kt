package no.nav.tilbakekreving

import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse

private val bigQueryService = BigQueryServiceStub()

fun opprettTilbakekreving(
    oppsamler: BehovObservatørOppsamler,
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
) = Tilbakekreving
    .opprett(
        behovObservatør = oppsamler,
        opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
        bigQueryService = bigQueryService,
        endringObservatør = EndringObservatørOppsamler(),
    )

fun tilbakekrevingTilBehandling(
    oppsamler: BehovObservatørOppsamler,
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
) = opprettTilbakekreving(oppsamler, opprettTilbakekrevingHendelse)
    .apply {
        håndter(kravgrunnlag())
        håndter(fagsysteminfoHendelse())
        håndter(brukerinfoHendelse())
        håndter(VarselbrevSendtHendelse(varselbrev()))
    }
