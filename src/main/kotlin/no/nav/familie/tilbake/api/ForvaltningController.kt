package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.util.UUID

@RestController
@RequestMapping("/api/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(private val forvaltningService: ForvaltningService) {

    @PutMapping(path = ["/behandling/{behandlingId}/kravgrunnlag/{kravgrunnlagId}/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
                        henteParam = "behandlingId")
    fun korrigerKravgrunnlag(@PathVariable behandlingId: UUID,
                             @PathVariable kravgrunnlagId: BigInteger): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId, kravgrunnlagId)
        return Ressurs.success("OK")
    }

    @PutMapping(path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Arkiverer mottatt kravgrunnlag",
                        henteParam = "behandlingId")
    fun arkiverMottattKravgrunnlag(@PathVariable mottattXmlId: UUID): Ressurs<String> {
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }
}