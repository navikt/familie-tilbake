package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
//@Unprotected
class TestController(val integrasjonerClient: IntegrasjonerClient) {

    @GetMapping(path = ["/{ident}"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sakjsbehandlernavn(ident: String): Saksbehandler {
        return integrasjonerClient.hentSaksbehandler(ident)

    }

}
