package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(private val forvaltningService: ForvaltningService) {

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(path = ["/behandling/{behandlingId}/kravgrunnlag/{kravgrunnlagId}/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                        handling = "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
                        henteParam = "behandlingId")
    fun korrigerKravgrunnlag(@PathVariable behandlingId: UUID,
                             @PathVariable kravgrunnlagId: BigInteger): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId, kravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Arkiver mottatt kravgrunnlag")
    @PutMapping(path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                        handling = "Arkiverer mottatt kravgrunnlag",
                        henteParam = "mottattXmlId")
    fun arkiverMottattKravgrunnlag(@PathVariable mottattXmlId: UUID): Ressurs<String> {
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Tvinghenlegg behandling")
    @PutMapping(path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                        handling = "Tving henlegger behandling",
                        henteParam = "behandlingId")
    fun tvingHenleggBehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent fagsysytemsbehandlingsinformasjon på nytt via Kafka")
    @PostMapping(path = ["/behandling/{behandlingId}/fagsystemsbehandling/v1"],
                     produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                        handling = "Henter fagsystemsbehandling på nytt",
                        henteParam = "behandlingId")
    fun hentFagsystemsbehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.hentFagsystemsbehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(path = ["/behandling/{behandlingId}/flytt-behandling/v1"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                        handling = "Flytter behandling tilbake til Fakta",
                        henteParam = "behandlingId")
    fun flyttBehandlingTilFakta(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }
}