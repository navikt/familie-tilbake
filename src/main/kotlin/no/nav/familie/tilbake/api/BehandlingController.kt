package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(val behandlingService: BehandlingService,
                           val stegService: StegService) {


    @PostMapping(path = ["/v1"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Oppretter tilbakekreving")
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

    @GetMapping(path = ["/v1/{behandlingId}"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter tilbakekrevingsbehandling",
                        henteParam = "behandlingId")
    fun hentBehandling(@NotNull @PathVariable("behandlingId") behandlingId: UUID): Ressurs<BehandlingDto> {
        return Ressurs.success(behandlingService.hentBehandling(behandlingId))
    }

    @PostMapping(path = ["{behandlingId}/steg/v1"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Håndterer behandlings aktiv steg og fortsetter den til neste steg")
    fun behandleSteg(@PathVariable("behandlingId") behandlingId: UUID) {
        stegService.håndterSteg(behandlingId)
    }
}
