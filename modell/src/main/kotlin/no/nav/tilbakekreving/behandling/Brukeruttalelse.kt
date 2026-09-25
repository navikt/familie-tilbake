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
    internal fun nyTilFrontendDto(etterForhåndsvarsel: Boolean): UttalelseDto {
        when (uttalelseVurdering) {
            UttalelseVurdering.JA -> {
                return UttalelseDto(
                    harBrukerUttaltSeg = when (etterForhåndsvarsel) {
                        true -> UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL
                        else -> UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG
                    },
                    uttalelsesdato = uttalelseInfo!!.uttalelsesdato,
                    hvorBrukerenUttalteSeg = uttalelseInfo.hvorBrukerenUttalteSeg,
                    beskrivelse = uttalelseInfo.uttalelseBeskrivelse,
                )
            }
            UttalelseVurdering.NEI -> {
                return UttalelseDto(
                    harBrukerUttaltSeg = when (etterForhåndsvarsel) {
                        true -> UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL
                        else -> UttalelseVurderingDto.UNNTAK_INGEN_UTTALELSE
                    },
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
)

enum class UttalelseVurdering(val meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>) {
    JA(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    NEI(emptySet()),
}
