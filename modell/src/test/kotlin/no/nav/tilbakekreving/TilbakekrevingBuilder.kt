package no.nav.tilbakekreving

import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.EnumMap
import java.util.UUID

private val bigQueryService = BigQueryServiceStub()

fun defaultFeatures(
    featureOverrides: Array<Pair<Toggle, Boolean>> = emptyArray(),
    fagsystemToggleOverrides: Array<Pair<FagsystemToggle, Boolean>> = emptyArray(),
): FeatureToggles = FeatureToggles(
    overrides = EnumMap<Toggle, Boolean>(Toggle::class.java).apply {
        putAll(featureOverrides)
    },
    fagsystemToggle = EnumMap<FagsystemDTO, EnumMap<FagsystemToggle, Boolean>>(
        FagsystemDTO.entries.associateWith {
            EnumMap<FagsystemToggle, Boolean>(FagsystemToggle::class.java).apply {
                putAll(fagsystemToggleOverrides.toMap())
            }
        },
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
    }
    return tilbakekreving
}
