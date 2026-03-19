package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import java.time.LocalDate
import java.util.UUID

class Brukeruttalelse(
    private val id: UUID,
    private val uttalelseVurdering: UttalelseVurdering,
    private val uttalelseInfo: List<UttalelseInfo>,
    private val kommentar: String?,
    private var trengerNyVurdering: Boolean,
) {
    fun trengerNyVurdering(): Boolean = trengerNyVurdering

    fun vurderPåNytt() {
        trengerNyVurdering = true
    }

    fun tilFrontendDto(): BrukeruttalelseDto {
        return BrukeruttalelseDto(
            harBrukerUttaltSeg = HarBrukerUttaltSeg.valueOf(uttalelseVurdering.name),
            uttalelsesdetaljer = uttalelseInfo.let { info ->
                info.map {
                    Uttalelsesdetaljer(
                        uttalelsesdato = it.uttalelsesdato,
                        hvorBrukerenUttalteSeg = it.hvorBrukerenUttalteSeg,
                        uttalelseBeskrivelse = it.uttalelseBeskrivelse,
                    )
                }
            },
            kommentar = kommentar,
        )
    }

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
        trengerNyVurdering = trengerNyVurdering,
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
    JA_ETTER_FORHÅNDSVARSEL(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    NEI_ETTER_FORHÅNDSVARSEL(emptySet()),
    UTTSETT_FRIST(emptySet()),
    UNNTAK_ALLEREDE_UTTALT_SEG(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
    UNNTAK_INGEN_UTTALELSE(emptySet()),

    @Deprecated("midreltidig, fjernes etter prodsatt og migrering")
    JA(setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),

    @Deprecated("midreltidig, fjernes etter prodsatt og migrering")
    NEI(emptySet()),
}
