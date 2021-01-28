package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(val behandlingService: BehandlingService) {


    @PostMapping(path = ["/v1"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@Valid @RequestBody
                          opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Ressurs<String> {
        val behandling = when {
            opprettTilbakekrevingRequest.manueltOpprettet -> {
                behandlingService.opprettBehandlingManuell(opprettTilbakekrevingRequest)
            }
            else -> {
                behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
            }
        }
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Behandling er opprettet.")
    }
}
