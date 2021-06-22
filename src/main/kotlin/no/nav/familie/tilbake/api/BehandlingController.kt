package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(private val behandlingService: BehandlingService,
                           private val stegService: StegService) {


    @PostMapping(path = ["/v1"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Oppretter tilbakekreving")
    fun opprettBehandling(@Valid @RequestBody
                          opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Ressurs<String> {
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        return Ressurs.success(behandling.eksternBrukId.toString(), melding = "Behandling er opprettet.")
    }

    @PostMapping(path = ["/manuelt/task/v1"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Oppretter tilbakekreving manuelt")
    fun opprettBehandlingManuellTask(@Valid @RequestBody
                                     opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest): Ressurs<String> {
        behandlingService.opprettManuellBehandlingTask(opprettManueltTilbakekrevingRequest)
        return Ressurs.success("Manuelt tilbakekrevingsbehandling opprettelse forespørselen innsendt")
    }


    @GetMapping(path = ["/v1/{behandlingId}"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter tilbakekrevingsbehandling",
                        henteParam = "behandlingId")
    fun hentBehandling(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<BehandlingDto> {
        return Ressurs.success(behandlingService.hentBehandling(behandlingId))
    }

    @PostMapping(path = ["{behandlingId}/steg/v1"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    // Rollen blir endret til BESLUTTER i Tilgangskontroll for FatteVedtak steg
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Utfører behandlingens aktiv steg og fortsetter den til neste steg",
                        henteParam = "behandlingId")
    fun utførBehandlingssteg(@PathVariable("behandlingId") behandlingId: UUID,
                             @Valid @RequestBody behandlingsstegDto: BehandlingsstegDto): Ressurs<String> {
        stegService.håndterSteg(behandlingId, behandlingsstegDto)
        if (behandlingsstegDto !is BehandlingsstegFatteVedtaksstegDto) {
            behandlingService.oppdaterAnsvarligSaksbehandler(behandlingId)
        }
        return Ressurs.success("OK")
    }

    @PutMapping(path = ["{behandlingId}/vent/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Setter saksbehandler behandling på vent eller utvider fristen",
                        henteParam = "behandlingId")
    fun settBehandlingPåVent(@PathVariable("behandlingId") behandlingId: UUID,
                             @Valid @RequestBody behandlingPåVentDto: BehandlingPåVentDto): Ressurs<String> {
        behandlingService.settBehandlingPåVent(behandlingId, behandlingPåVentDto)
        return Ressurs.success("OK")
    }

    @PutMapping(path = ["{behandlingId}/gjenoppta/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Saksbehandler tar behandling av vent etter å motta brukerrespons eller dokumentasjon",
                        henteParam = "behandlingId")
    fun taBehandlingAvVent(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        behandlingService.taBehandlingAvvent(behandlingId)
        return Ressurs.success("OK")
    }

    @PutMapping(path = ["{behandlingId}/henlegg/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Saksbehandler henlegger behandling",
                        henteParam = "behandlingId")
    fun henleggBehandling(@PathVariable("behandlingId") behandlingId: UUID,
                          @Valid @RequestBody henleggelsesbrevFritekstDto: HenleggelsesbrevFritekstDto): Ressurs<String> {
        behandlingService.henleggBehandling(behandlingId, henleggelsesbrevFritekstDto)
        return Ressurs.success("OK")
    }
}
