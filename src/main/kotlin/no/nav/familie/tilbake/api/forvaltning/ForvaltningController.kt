package no.nav.familie.tilbake.api.forvaltning

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.util.UUID

// Denne kontrollen inneholder tjenester som kun brukes av forvaltningsteam via swagger. Frontend bør ikke kalle disse tjenestene.

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

    @PutMapping(path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Tving henlegger behandling",
                        henteParam = "behandlingId")
    fun tvingHenleggBehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @PostMapping(path = ["/behandling/{behandlingId}/fagsystemsbehandling/v1"],
                     produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter fagsystemsbehandling på nytt",
                        henteParam = "behandlingId")
    fun hentFagsystemsbehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.hentFagsystemsbehandling(behandlingId)
        return Ressurs.success("OK")
    }
}