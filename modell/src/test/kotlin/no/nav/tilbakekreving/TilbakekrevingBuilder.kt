package no.nav.tilbakekreving

import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import java.util.EnumMap
import java.util.UUID

private val bigQueryService = BigQueryServiceStub()

fun defaultFeatures(
    vararg overrides: Pair<Toggle, Boolean>,
): FeatureToggles = FeatureToggles(
    EnumMap(
        mutableMapOf(
            Toggle.SendVarselbrev to true,
        ).apply { putAll(overrides) },
    ),
)

fun opprettTilbakekreving(
    oppsamler: BehovObservatørOppsamler,
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    features: FeatureToggles = defaultFeatures(),
) = Tilbakekreving
    .opprett(
        id = UUID.randomUUID().toString(),
        behovObservatør = oppsamler,
        opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
        bigQueryService = bigQueryService,
        endringObservatør = EndringObservatørOppsamler(),
        features = features,
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
        håndter(varselbrevHendelse(tilbakekreving.brevHistorikk.nåværende().entry.id))
    }
    return tilbakekreving
}
