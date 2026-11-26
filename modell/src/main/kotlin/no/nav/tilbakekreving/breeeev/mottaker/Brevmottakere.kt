package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.entities.BrevmottakereEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakereDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedmottakerAddresseDto
import java.time.LocalDateTime

class Brevmottakere(
    private var hovedmottaker: Hovedmottaker,
    private val tilleggsmottaker: Tilleggsmottaker?,
) {
    companion object {
        internal fun opprett() = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = null,
        )
    }

    internal fun tilEntity(tilbakekrevingRef: String) = BrevmottakereEntity(
        tilbakekrevingRef = tilbakekrevingRef,
        hovedmottaker = hovedmottaker.tilEntity(tilbakekrevingRef, null, null),
        tilleggsmottaker = tilleggsmottaker?.tilEntity(tilbakekrevingRef),
    )

    fun oppdaterBrevmottaker(sideeffektContext: SideeffektContext, hovedmottaker: Hovedmottaker): HovedmottakerAddresseDto {
        this.hovedmottaker = hovedmottaker
        sideeffektContext.behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = null,
                opprettetTid = LocalDateTime.now(),
                behandlingsloggstype = Behandlingsloggstype.BREVMOTTAKER_OPPDATERT,
                rolle = sideeffektContext.behandler.rolle,
                behandlerIdent = sideeffektContext.behandler.ident,
                ekstraInfo = emptyMap(),
            ),
        )
        return hovedmottaker.tilFrontendDto()
    }

    fun tilFrontendDto() = BrevmottakereDto(
        hovedmottaker = hovedmottaker.tilFrontendDto(),
        tilleggsmottaker = tilleggsmottaker?.tilFrontendDto(),
    )
}
