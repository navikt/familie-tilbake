package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.VurdertForeldelseDto
import no.nav.familie.tilbake.foreldelse.ForeldelseService
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

@RestController
@RequestMapping("/api/behandling/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForeldelseController(val foreldelseService: ForeldelseService) {

    @GetMapping(path = ["{behandlingId}/foreldelse/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter foreldelsesinformasjon for en gitt behandling",
                        henteParam = "behandlingId")
    fun hentVurdertForeldelse(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<VurdertForeldelseDto> {
        return Ressurs.success(foreldelseService.hentVurdertForeldelse(behandlingId))
    }

}
