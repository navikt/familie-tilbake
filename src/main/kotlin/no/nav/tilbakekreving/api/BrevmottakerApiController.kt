package no.nav.tilbakekreving.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.brev.mottaker.Poststed
import no.nav.tilbakekreving.kontrakter.frontend.apis.BrevmottakerApi
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakerApiController : BrevmottakerApi {
    override fun brevmottakerHentPoststeder(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(Poststed.alle)
    }
}
