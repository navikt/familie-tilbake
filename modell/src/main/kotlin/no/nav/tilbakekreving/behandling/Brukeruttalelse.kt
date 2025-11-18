package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UtsettFristInfoEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import java.time.LocalDate
import java.util.UUID

class Brukeruttalelse(
    val id: UUID,
    val uttalelseVurdering: UttalelseVurdering,
    val uttalelseInfo: List<UttalelseInfo>,
    val kommentar: String?,
    val utsettUttalselsFrist: List<UtsettFristInfo>,
) {
    fun tilEntity(behandlingRef: UUID): BrukeruttalelseEntity = BrukeruttalelseEntity(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        behandlingRef = behandlingRef,
        uttalelseInfoEntity = uttalelseInfo.map {
            UttalelseInfoEntity(
                id = UUID.randomUUID(),
                brukeruttalelseRef = id,
                uttalelsesdato = it.uttalelsesdato,
                hvorBrukerenUttalteSeg = it.hvorBrukerenUttalteSeg,
                uttalelseBeskrivelse = it.uttalelseBeskrivelse,
            )
        },
        kommentar = kommentar,
        utsettFristEntity = utsettUttalselsFrist.map {
            UtsettFristInfoEntity(
                id = UUID.randomUUID(),
                brukeruttalelseRef = id,
                nyFrist = it.nyFrist,
                begrunnelse = it.begrunnelse,
            )
        },
    )
}

data class UtsettFristInfo(
    val id: UUID,
    val nyFrist: LocalDate,
    val begrunnelse: String,
)

data class UttalelseInfo(
    val id: UUID,
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
)

enum class UttalelseVurdering {
    JA,
    NEI,
    UTTSETT_FRIST,
}
