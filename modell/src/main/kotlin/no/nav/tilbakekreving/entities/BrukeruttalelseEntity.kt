package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Brukeruttalelse
import no.nav.tilbakekreving.behandling.UtsettFristInfo
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import java.time.LocalDate
import java.util.UUID

data class BrukeruttalelseEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val uttalelseVurdering: UttalelseVurdering,
    val uttalelseInfoEntity: List<UttalelseInfoEntity>,
    val utsettFristEntity: List<UtsettFristInfoEntity>,
    val kommentar: String?,
) {
    fun fraEntity(): Brukeruttalelse = Brukeruttalelse(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        uttalelseInfo = uttalelseInfoEntity.map { it.fraEntity() },
        kommentar = kommentar,
        utsettUttalselsFrist = utsettFristEntity.map { it.fraEntity() },
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

data class UtsettFristInfoEntity(
    val id: UUID,
    val brukeruttalelseRef: UUID,
    val nyFrist: LocalDate,
    val begrunnelse: String,
) {
    fun fraEntity(): UtsettFristInfo = UtsettFristInfo(id, nyFrist, begrunnelse)
}
