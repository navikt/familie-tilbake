package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FaktaFeilutbetalingController(val faktaFeilutbetalingService: FaktaFeilutbetalingService) {

    @GetMapping(path = ["/behandling/{behandlingId}/fakta/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter fakta om feilutbetaling for en gitt behandling",
                        henteParam = "behandlingId")
    fun hentFaktaomfeilutbetaling(@NotNull @PathVariable("behandlingId") behandlingId: UUID): Ressurs<FaktaFeilutbetalingDto> {
        return Ressurs.success(faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId))
    }

}
