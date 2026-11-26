package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.entities.BrevmottakereEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakereDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedmottakerAddresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilleggsmottakerDto
import java.time.LocalDateTime
import java.util.UUID

class Brevmottakere(
    private var hovedmottaker: Hovedmottaker,
    private var tilleggsmottaker: Tilleggsmottaker?,
) {
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

    fun oppdaterTilleggsmottaker(sideeffektContext: SideeffektContext, tilleggsmottaker: Tilleggsmottaker): TilleggsmottakerDto {
        val logginnslagType = when (this.tilleggsmottaker) {
            null -> when (tilleggsmottaker.mottakerType) {
                Tilleggsmottaker.TilleggsmottakerType.Verge -> Behandlingsloggstype.VERGE_LAGT_TIL
                Tilleggsmottaker.TilleggsmottakerType.Fullmektig -> Behandlingsloggstype.FULLMEKTIG_LAGT_TIL
            }
            else -> when (tilleggsmottaker.mottakerType) {
                Tilleggsmottaker.TilleggsmottakerType.Verge -> Behandlingsloggstype.VERGE_ENDRET
                Tilleggsmottaker.TilleggsmottakerType.Fullmektig -> Behandlingsloggstype.FULLMEKTIG_ENDRET
            }
        }

        this.tilleggsmottaker = tilleggsmottaker

        sideeffektContext.behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = null,
                opprettetTid = LocalDateTime.now(),
                behandlingsloggstype = logginnslagType,
                rolle = sideeffektContext.behandler.rolle,
                behandlerIdent = sideeffektContext.behandler.ident,
                ekstraInfo = emptyMap(),
            ),
        )
        return tilleggsmottaker.tilFrontendDto()
    }

    fun tilFrontendDto() = BrevmottakereDto(
        hovedmottaker = hovedmottaker.tilFrontendDto(),
        tilleggsmottaker = tilleggsmottaker?.tilFrontendDto(),
    )

    fun fjernTilleggsmottaker(sideeffektContext: SideeffektContext, brevmottakerId: UUID, sporing: Sporing) {
        if (tilleggsmottaker?.gjelder(brevmottakerId) != true) {
            throw ModellFeil.IkkeFunnetException("Kunne ikke finne hvilken verge eller fullmektig som skal fjernes. Oppdater siden og prøv på nytt.", sporing)
        }

        sideeffektContext.behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = null,
                opprettetTid = LocalDateTime.now(),
                behandlingsloggstype = when (tilleggsmottaker!!.mottakerType) {
                    Tilleggsmottaker.TilleggsmottakerType.Verge -> Behandlingsloggstype.VERGE_FJERNET
                    Tilleggsmottaker.TilleggsmottakerType.Fullmektig -> Behandlingsloggstype.FULLMEKTIG_FJERNET
                },
                rolle = sideeffektContext.behandler.rolle,
                behandlerIdent = sideeffektContext.behandler.ident,
                ekstraInfo = emptyMap(),
            ),
        )

        tilleggsmottaker = null
    }

    companion object {
        internal fun opprett() = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = null,
        )
    }
}
