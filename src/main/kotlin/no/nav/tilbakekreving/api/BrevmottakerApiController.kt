package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.breeeev.mottaker.Addresse
import no.nav.tilbakekreving.breeeev.mottaker.Hovedmottaker
import no.nav.tilbakekreving.brev.mottaker.Poststed
import no.nav.tilbakekreving.kontrakter.frontend.apis.BrevmottakerApi
import no.nav.tilbakekreving.kontrakter.frontend.models.FregAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedmottakerAddresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NorskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenlandskAdresseDto
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

    private fun HovedmottakerAddresseDto.tilDomene(): Hovedmottaker = when (this) {
        is FregAdresseDto -> Hovedmottaker.FolkeregistrertAddresse
        is NorskAdresseDto -> Addresse.Norsk(
            id = UUID.randomUUID(),
            addresselinje = gate,
            postnummer = postnummer,
        )
        is UtenlandskAdresseDto -> Addresse.Utenlandsk(
            id = UUID.randomUUID(),
            land = land,
            addresselinje = adresse,
        )
    }
}
