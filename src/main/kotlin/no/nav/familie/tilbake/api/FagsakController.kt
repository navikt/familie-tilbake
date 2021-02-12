package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.FagsakResponsDto
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/fagsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(val fagsakService: FagsakService) {

    @GetMapping(path = ["/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter fagsak informasjon med bruker og behandlinger",
                        henteParam = "ytelsestype")
    fun hentFagsak(@NotNull @RequestParam("ytelse") ytelsestype: Ytelsestype,
                   @NotNull @RequestParam("fagsak") eksternFagsakId: String): Ressurs<FagsakResponsDto> {
        return Ressurs.success(fagsakService.hentFagsak(ytelsestype, eksternFagsakId))
    }
}
