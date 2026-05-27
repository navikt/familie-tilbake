package no.nav.tilbakekreving

import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.EnumMap
import java.util.UUID

fun defaultFeatures(
    featureOverrides: Array<Pair<Toggle, Boolean>> = emptyArray(),
    fagsystemToggleOverrides: Array<Pair<FagsystemToggle, Boolean>> = emptyArray(),
): FeatureToggles = FeatureToggles(
    overrides = EnumMap<Toggle, Boolean>(Toggle::class.java).apply {
        putAll(featureOverrides)
    },
    fagsystemToggles = EnumMap<FagsystemDTO, EnumMap<FagsystemToggle, Boolean>>(
        FagsystemDTO.entries.associateWith {
            EnumMap<FagsystemToggle, Boolean>(FagsystemToggle::class.java).apply {
                putAll(fagsystemToggleOverrides.toMap())
            }
        },
    ),
)

fun opprettTilbakekreving(
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    context: SideeffektContext = systemContext(),
) = Tilbakekreving
    .opprett(
        id = UUID.randomUUID().toString(),
        opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
        sideeffektContext = context,
    )

fun tilbakekrevingTilBehandling(
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    context: SideeffektContext = systemContext(),
): Tilbakekreving {
    val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context)
    tilbakekreving.apply {
        håndter(kravgrunnlag(), context)
        håndter(fagsysteminfoHendelse(), context)
        håndter(brukerinfoHendelse(), context)
    }
    return tilbakekreving
}
