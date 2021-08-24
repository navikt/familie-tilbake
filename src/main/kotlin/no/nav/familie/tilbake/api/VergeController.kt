package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.VergeService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/behandling/v1/{behandlingId}/verge", produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VergeController(private val vergeService: VergeService) {

    @PostMapping
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Oppretter verge på behandling og deaktiverer ev. eksisterende verge.",
                        henteParam = "behandlingId")
    fun opprettVerge(@PathVariable("behandlingId") behandlingId: UUID,
                     @Valid @RequestBody vergeDto: VergeDto): Ressurs<String> {
        vergeService.opprettVerge(behandlingId, vergeDto)
        return Ressurs.success("OK")
    }

    @PutMapping
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Deaktiverer ev. eksisterende verge.",
                        henteParam = "behandlingId")
    fun fjernVerge(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<String> {
        vergeService.fjernVerge(behandlingId)
        return Ressurs.success("OK")
    }


}