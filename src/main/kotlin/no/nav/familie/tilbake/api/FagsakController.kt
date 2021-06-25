package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.api.dto.FinnesBehandlingsresponsDto
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService) {

    @GetMapping(path = ["/fagsystem/{fagsystem}/fagsak/{fagsak}/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter fagsak informasjon med bruker og behandlinger",
                        henteParam = "fagsystem")
    fun hentFagsak(@PathVariable("fagsystem") fagsystem: Fagsystem,
                   @PathVariable("fagsak") eksternFagsakId: String): Ressurs<FagsakDto> {
        return Ressurs.success(fagsakService.hentFagsak(fagsystem, eksternFagsakId))
    }

    @GetMapping(path = ["/fagsystem/{fagsystem}/fagsak/{fagsak}/finnesApenBehandling/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Sjekk om det finnes en åpen tilbakekrevingsbehandling",
                        henteParam = "fagsystem")
    fun finnesÅpenTilbakekrevingsbehandling(@PathVariable("fagsystem") fagsystem: Fagsystem,
                                            @PathVariable("fagsak")
                                            eksternFagsakId: String): Ressurs<FinnesBehandlingsresponsDto> {
        return Ressurs.success(fagsakService.finnesÅpenTilbakekrevingsbehandling(fagsystem = fagsystem,
                                                                                 eksternFagsakId = eksternFagsakId))
    }

    @GetMapping(path = ["/ytelsestype/{ytelsestype}/fagsak/{fagsak}/kanBehandlingOpprettesManuelt/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Sjekk om det er mulig å opprette behandling manuelt",
                        henteParam = "ytelsestype")
    fun kanBehandlingOpprettesManuelt(@PathVariable ytelsestype: Ytelsestype,
                                      @PathVariable("fagsak")
                                      eksternFagsakId: String): Ressurs<KanBehandlingOpprettesManueltRespons> {
        return Ressurs.success(fagsakService.kanBehandlingOpprettesManuelt(eksternFagsakId, ytelsestype))
    }

    @GetMapping(path = ["/fagsystem/{fagsystem}/fagsak/{fagsak}/behandlinger/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter behandlinger for bruk i fagsystem",
                        henteParam = "fagsystem")
    fun hentBehandlingerForFagsystem(@PathVariable("fagsystem") fagsystem: Fagsystem,
                                     @PathVariable("fagsak")
                                     eksternFagsakId: String): Ressurs<List<Behandling>> {
        return Ressurs.success(fagsakService.hentBehandlingerForFagsak(fagsystem, eksternFagsakId));
    }
}
