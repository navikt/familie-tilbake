package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Brukeruttalelse
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import java.time.LocalDate
import java.util.UUID

data class BrukeruttalelseEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val uttalelseVurdering: UttalelseVurdering,
    val uttalelseInfoEntity: List<UttalelseInfoEntity>,
    val utsettFrist: LocalDate?,
    val beskrivelseVedNeiEllerUtsettFrist: String?,
) {
    fun fraEntity(): Brukeruttalelse = Brukeruttalelse(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        uttalelseInfo = uttalelseInfoEntity.map { it.fraEntity() },
        beskrivelseVedNeiEllerUtsettFrist = beskrivelseVedNeiEllerUtsettFrist,
        utsettFrist = utsettFrist,
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
