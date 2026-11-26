package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.breeeev.mottaker.Addresse
import no.nav.tilbakekreving.breeeev.mottaker.Hovedmottaker
import no.nav.tilbakekreving.breeeev.mottaker.ManuellAddresse
import no.nav.tilbakekreving.breeeev.mottaker.Tilleggsmottaker
import no.nav.tilbakekreving.brev.mottaker.Poststed
import no.nav.tilbakekreving.kontrakter.frontend.apis.BrevmottakerApi
import no.nav.tilbakekreving.kontrakter.frontend.models.FregAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FullmektigDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedmottakerAddresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ManuellAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ManuellMottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NorskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OrganisasjonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PersonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilleggsmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenlandskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VergeDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakerApiController(
    private val tilbakekrevingService: TilbakekrevingService,
) : BrevmottakerApi {
    override fun brevmottakerHentPoststeder(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(Poststed.alle)
    }

    override fun brevmottakerEndreHovedmottaker(
        tilbakekrevingId: String,
        hovedmottakerAddresseDto: HovedmottakerAddresseDto,
    ): ResponseEntity<HovedmottakerAddresseDto> {
        val hovedmottaker = hovedmottakerAddresseDto.tilDomene()
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.tilbakekreving(tilbakekrevingId),
            valideringContext = ValideringContext.EndreBrevmottaker,
        ) { tilbakekreving, sideeffektContext ->
            ResponseEntity.ok(tilbakekreving.brevmottakere.oppdaterBrevmottaker(sideeffektContext, hovedmottaker))
        } ?: ResponseEntity.notFound().build()
    }

    override fun brevmottakerEndreTilleggsmottaker(tilbakekrevingId: String, brevmottakerId: UUID, tilleggsmottakerDto: TilleggsmottakerDto): ResponseEntity<TilleggsmottakerDto> {
        return brevmottakerLeggTilTilleggsmottaker(tilbakekrevingId, tilleggsmottakerDto)
    }

    override fun brevmottakerLeggTilTilleggsmottaker(tilbakekrevingId: String, tilleggsmottakerDto: TilleggsmottakerDto): ResponseEntity<TilleggsmottakerDto> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.tilbakekreving(tilbakekrevingId),
            valideringContext = ValideringContext.EndreTilleggsmottaker,
        ) { tilbakekreving, sideeffektContext ->
            ResponseEntity.ok(tilbakekreving.brevmottakere.oppdaterTilleggsmottaker(sideeffektContext, tilDomene(tilleggsmottakerDto)))
        } ?: ResponseEntity.notFound().build()
    }

    override fun brevmottakerSlettBrevmottaker(tilbakekrevingId: String, brevmottakerId: UUID): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.tilbakekreving(tilbakekrevingId),
            valideringContext = ValideringContext.FjernTilleggsmottaker,
        ) { tilbakekreving, sideeffektContext ->
            ResponseEntity.ok(tilbakekreving.brevmottakere.fjernTilleggsmottaker(sideeffektContext, brevmottakerId, tilbakekreving.sporingsinformasjon()))
        } ?: ResponseEntity.notFound().build()
    }

    private fun tilDomene(tilleggsmottakerDto: TilleggsmottakerDto): Tilleggsmottaker = when (tilleggsmottakerDto) {
        is VergeDto -> when (val mottaker = tilleggsmottakerDto.mottaker) {
            is ManuellAdresseDto -> Tilleggsmottaker.VergeMedManuellAddresse(
                navn = mottaker.navn,
                addresse = mottaker.mottaker.tilDomene(tilleggsmottakerDto.id),
            )
            is PersonDto -> Tilleggsmottaker.VergeMedFolkeregistrertAddresse(
                id = tilleggsmottakerDto.id,
                fødselsnummer = mottaker.ident,
            )
        }
        is FullmektigDto -> when (val mottaker = tilleggsmottakerDto.mottaker) {
            is ManuellAdresseDto -> Tilleggsmottaker.FullmektigMedManuellAddresse(
                navn = mottaker.navn,
                addresse = mottaker.mottaker.tilDomene(tilleggsmottakerDto.id),
            )
            is OrganisasjonDto -> Tilleggsmottaker.FullmektigMedOrganisasjonsnummer(
                id = tilleggsmottakerDto.id,
                navn = mottaker.navn ?: "",
                organisasjonsnummer = mottaker.organisasjonsnummer,
            )
            is PersonDto -> Tilleggsmottaker.FullmektigPerson(
                id = tilleggsmottakerDto.id,
                fødselsnummer = mottaker.ident,
            )
        }
    }

    private fun ManuellMottakerDto.tilDomene(id: UUID): ManuellAddresse = when (this) {
        is NorskAdresseDto -> Addresse.Norsk(id = id, addresselinje = gate, postnummer = postnummer)
        is UtenlandskAdresseDto -> Addresse.Utenlandsk(id = id, land = land, addresselinje = adresse)
    }

    private fun HovedmottakerAddresseDto.tilDomene(): Hovedmottaker = when (this) {
        is FregAdresseDto -> Hovedmottaker.FolkeregistrertAddresse
        is ManuellMottakerDto -> tilDomene(UUID.randomUUID()) as Hovedmottaker
    }
}
