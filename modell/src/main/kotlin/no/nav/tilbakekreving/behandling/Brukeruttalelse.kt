package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import java.time.LocalDate
import java.util.UUID

class Brukeruttalelse(
    private val id: UUID,
    private val uttalelseVurdering: UttalelseVurdering,
    private val uttalelseInfo: UttalelseInfo?,
    private val kommentar: String?,
) {
    fun klon(): Brukeruttalelse {
        return Brukeruttalelse(
            id = id,
            uttalelseVurdering = uttalelseVurdering,
            uttalelseInfo = uttalelseInfo?.klon(),
            kommentar = kommentar,
        )
    }

    internal fun nyTilFrontendDto(): UttalelseDto {
        when (uttalelseVurdering) {
            UttalelseVurdering.JA -> {
                return UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.JA,
                    uttalelsesdato = uttalelseInfo!!.uttalelsesdato,
                    hvorBrukerenUttalteSeg = uttalelseInfo.hvorBrukerenUttalteSeg,
                    beskrivelse = uttalelseInfo.uttalelseBeskrivelse,
                )
            }
            UttalelseVurdering.NEI -> {
                return UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.NEI,
                    beskrivelse = kommentar,
                )
            }
        }
    }

    fun tilEntity(behandlingRef: UUID): BrukeruttalelseEntity = BrukeruttalelseEntity(
        id = id,
        uttalelseVurdering = uttalelseVurdering,
        behandlingRef = behandlingRef,
        uttalelseInfoEntity = uttalelseInfo?.let {
            UttalelseInfoEntity(
                id = UUID.randomUUID(),
                brukeruttalelseRef = id,
                uttalelsesdato = it.uttalelsesdato,
                hvorBrukerenUttalteSeg = it.hvorBrukerenUttalteSeg,
                uttalelseBeskrivelse = it.uttalelseBeskrivelse,
            )
        },
        kommentar = kommentar,
    )

    fun meldingerTilSaksbehandler() = uttalelseVurdering.meldingerTilSaksbehandler
}

data class UttalelseInfo(
    val id: UUID,
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
) {
    fun klon(): UttalelseInfo {
        return UttalelseInfo(
            id = id,
            uttalelsesdato = uttalelsesdato,
            hvorBrukerenUttalteSeg = hvorBrukerenUttalteSeg,
            uttalelseBeskrivelse = uttalelseBeskrivelse,
        )
    }
}

enum class UttalelseVurdering(val meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>) {
    JA(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    NEI(emptySet()),
}
