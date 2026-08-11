package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Brukeruttalelse
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import java.time.LocalDate
import java.util.UUID

data class BrukeruttalelseEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val uttalelseVurdering: UttalelseVurdering,
    val uttalelseInfoEntity: UttalelseInfoEntity?,
    val kommentar: String?,
    val tilbakeført: ÅrsakTilTilbakeføring?,
) {
    fun fraEntity(): Brukeruttalelse = Brukeruttalelse(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        uttalelseInfo = uttalelseInfoEntity?.fraEntity(),
        kommentar = kommentar,
        tilbakeført = tilbakeført,
    )
}

data class UttalelseInfoEntity(
    val id: UUID,
    val brukeruttalelseRef: UUID,
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
) {
    fun fraEntity(): UttalelseInfo = UttalelseInfo(id, uttalelsesdato, hvorBrukerenUttalteSeg, uttalelseBeskrivelse)
}
