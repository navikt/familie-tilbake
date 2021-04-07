package no.nav.familie.tilbake.api.e2e

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.BeregnetPerioderDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/behandling/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(val tilbakekrevingsberegningService: TilbakekrevingsberegningService) {

    @PostMapping(path = ["{behandlingId}/beregn/v1"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Beregner feilutbetalt beløp for nye delte perioder",
                        henteParam = "behandlingId")
    fun beregnBeløp(@PathVariable("behandlingId") behandlingId: UUID,
                    @Valid @RequestBody perioder: List<PeriodeDto>): Ressurs<BeregnetPerioderDto> {
        return Ressurs.success(tilbakekrevingsberegningService.beregnBeløp(behandlingId, perioder))
    }
}
