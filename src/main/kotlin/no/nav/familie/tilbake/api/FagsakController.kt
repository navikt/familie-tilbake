package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
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
class FagsakController(val fagsakService: FagsakService) {

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
    fun finnesÅpenTilbakekrevingsbehandling(
            @PathVariable("fagsystem") fagsystem: Fagsystem,
            @PathVariable("fagsak") eksternFagsakId: String): Ressurs<FinnesBehandlingsresponsDto> {
        return Ressurs.success(fagsakService.finnesÅpenTilbakekrevingsbehandling(fagsystem = fagsystem,
                                                                                 eksternFagsakId = eksternFagsakId))
    }
}
