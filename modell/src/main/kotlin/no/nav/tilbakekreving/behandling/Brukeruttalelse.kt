package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import java.time.LocalDate
import java.util.UUID

class Brukeruttalelse(
    val id: UUID,
    val uttalelseVurdering: UttalelseVurdering,
    val uttalelseInfo: List<UttalelseInfo>?,
    val beskrivelseVedNeiEllerUtsettFrist: String?,
    val utsettFrist: LocalDate?,
) {
    fun tilEntity(behandlingRef: UUID): BrukeruttalelseEntity = BrukeruttalelseEntity(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        behandlingRef = behandlingRef,
        uttalelseInfoEntity = uttalelseInfo?.map {
            UttalelseInfoEntity(
                id = UUID.randomUUID(),
                brukeruttalelseRef = id,
                uttalelsesdato = it.uttalelsesdato,
                hvorBrukerenUttalteSeg = it.hvorBrukerenUttalteSeg,
                uttalelseBeskrivelse = it.uttalelseBeskrivelse,
            )
        },
        beskrivelseVedNeiEllerUtsettFrist = beskrivelseVedNeiEllerUtsettFrist,
        utsettFrist = utsettFrist,
    )
}

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
