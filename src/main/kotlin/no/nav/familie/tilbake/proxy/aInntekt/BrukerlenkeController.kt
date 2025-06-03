package no.nav.familie.tilbake.proxy.aInntekt

import no.nav.familie.tilbake.kontrakter.PersonIdent
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.BrukerlenkeDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brukerlenke")
@ProtectedWithClaims(issuer = "azuread")
class BrukerlenkeController(
    private val service: BrukerlenkeService,
) {
    /**
     * Brukes for å generere en url til arbeid-og-inntekt
     * for å kunne sende saksbehandleren til identen sin side på arbeid og inntekt
     */
    @GetMapping
    fun hentUrlTilArbeidOgInntekt(
        @RequestHeader("x-person-ident") personIdent: PersonIdent,
        @RequestHeader("x-fagsak-id") fagsakId: String?,
        @RequestHeader("x-behandling-id") behandlingId: String?,
    ): Ressurs<BrukerlenkeDto> = Ressurs.success(BrukerlenkeDto(url = service.hentAInntektUrl(personIdent.ident, fagsakId, behandlingId)))
}
