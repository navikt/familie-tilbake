package no.nav.familie.tilbake.proxy.aInntekt

import no.nav.familie.tilbake.kontrakter.PersonIdent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.ProxyLenkeDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/arbeid-og-inntekt")
@ProtectedWithClaims(issuer = "azuread")
class ArbeidOgInntektController(
    private val service: ArbeidOgInntektService,
) {
    /**
     * Brukes for å generere en url til arbeid-og-inntekt
     * for å kunne sende saksbehandleren til identen sin side på arbeid og inntekt
     */
    @GetMapping("/brukerlenke")
    fun hentUrlTilArbeidOgInntekt(
        @RequestHeader("x-person-ident") personIdent: PersonIdent,
        @RequestHeader("x-fagsak-id") fagsakId: String?,
        @RequestHeader("x-behandling-id") behandlingId: String?,
    ): ProxyLenkeDto {
        val logContext = SecureLog.Context.medBehandling(fagsakId, behandlingId)
        return ProxyLenkeDto(ainntektUrl = service.hentAInntektUrl(personIdent.ident, logContext))
    }
}
