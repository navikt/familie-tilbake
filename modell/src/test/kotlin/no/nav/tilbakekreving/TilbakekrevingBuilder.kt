package no.nav.tilbakekreving

import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import java.util.UUID

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
