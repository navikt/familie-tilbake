package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.BrukeruttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IngenUttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TidligereBrukeruttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import java.time.LocalDate
import java.util.UUID

class Brukeruttalelse(
    private val id: UUID,
    private val uttalelseVurdering: UttalelseVurdering,
    private val uttalelseInfo: UttalelseInfo?,
    private val kommentar: String?,
) {
    internal fun nyTilFrontendDto(): UttalelseVurderingDto {
        when (uttalelseVurdering) {
            UttalelseVurdering.JA -> {
                return BrukeruttalelseDto(
                    uttalelsesdato = uttalelseInfo!!.uttalelsesdato,
                    hvorBrukerenUttalteSeg = uttalelseInfo.hvorBrukerenUttalteSeg,
                    beskrivelse = uttalelseInfo.uttalelseBeskrivelse,
                )
            }
            UttalelseVurdering.TILBAKEFØRT -> {
                return TidligereBrukeruttalelseDto(
                    uttalelsesdato = uttalelseInfo!!.uttalelsesdato,
                    hvorBrukerenUttalteSeg = uttalelseInfo.hvorBrukerenUttalteSeg,
                    beskrivelse = uttalelseInfo.uttalelseBeskrivelse,
                )
            }
            UttalelseVurdering.NEI -> {
                return IngenUttalelseDto(
                    kommentar = kommentar!!,
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

    fun trengerNyVurdering(): Brukeruttalelse? {
        return when (uttalelseVurdering) {
            UttalelseVurdering.NEI -> null
            UttalelseVurdering.JA -> Brukeruttalelse(
                id = id,
                kommentar = null,
                uttalelseVurdering = UttalelseVurdering.TILBAKEFØRT,
                uttalelseInfo = uttalelseInfo,
            )
            UttalelseVurdering.TILBAKEFØRT -> this
        }
    }
}

data class UttalelseInfo(
    val id: UUID,
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
)

enum class UttalelseVurdering(val meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>) {
    JA(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    TILBAKEFØRT(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    NEI(emptySet()),
}
