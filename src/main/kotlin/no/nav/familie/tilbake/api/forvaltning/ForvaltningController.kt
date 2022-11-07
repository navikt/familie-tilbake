package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.HentFagsystemsbehandlingRequestDto
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.util.UUID
import javax.validation.Valid

// Denne kontrollen inneholder tjenester som kun brukes av forvaltningsteam via swagger. Frontend bør ikke kalle disse tjenestene.

@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(private val forvaltningService: ForvaltningService) {

    @Operation(summary = "Hent korrigert kravgrunnlag")
    @PutMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/{kravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter korrigert kravgrunnlag fra økonomi og oppdaterer kravgrunnlag431",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID
    )
    fun korrigerKravgrunnlag(
        @PathVariable behandlingId: UUID,
        @PathVariable kravgrunnlagId: BigInteger
    ): Ressurs<String> {
        forvaltningService.korrigerKravgrunnlag(behandlingId, kravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Arkiver mottatt kravgrunnlag")
    @PutMapping(
        path = ["/arkiver/kravgrunnlag/{mottattXmlId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Arkiverer mottatt kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.MOTTATT_XML_ID
    )
    fun arkiverMottattKravgrunnlag(@PathVariable mottattXmlId: UUID): Ressurs<String> {
        forvaltningService.arkiverMottattKravgrunnlag(mottattXmlId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Tvinghenlegg behandling")
    @PutMapping(
        path = ["/behandling/{behandlingId}/tving-henleggelse/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(Behandlerrolle.FORVALTER, "Tving henlegger behandling", AuditLoggerEvent.NONE, HenteParam.BEHANDLING_ID)
    fun tvingHenleggBehandling(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.tvingHenleggBehandling(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent fagsysytemsbehandlingsinformasjon fra fagsystem via Kafka")
    @PostMapping(
        path = ["/fagsystemsbehandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(Behandlerrolle.FORVALTER, "Henter fagsystemsbehandling fra fagsystem via kafka", AuditLoggerEvent.UPDATE)
    fun hentFagsystemsbehandling(
        @Valid @RequestBody
        hentFagsystemsbehandlingRequest: HentFagsystemsbehandlingRequestDto
    ): Ressurs<String> {
        forvaltningService.hentFagsystemsbehandling(hentFagsystemsbehandlingRequest)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Flytt behandling tilbake til fakta")
    @PutMapping(
        path = ["/behandling/{behandlingId}/flytt-behandling/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Flytter behandling tilbake til Fakta",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID
    )
    fun flyttBehandlingTilFakta(@PathVariable behandlingId: UUID): Ressurs<String> {
        forvaltningService.flyttBehandlingsstegTilbakeTilFakta(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Annuler kravgrunnlag")
    @PutMapping(
        path = ["/annuler/kravgrunnlag/{eksternKravgrunnlagId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Annulerer kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.EKSTERN_KRAVGRUNNLAG_ID
    )
    fun annulerKravgrunnlag(@PathVariable eksternKravgrunnlagId: BigInteger): Ressurs<String> {
        forvaltningService.annulerKravgrunnlag(eksternKravgrunnlagId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Hent informasjon som kreves for forvaltning")
    @GetMapping(
        path = ["/ytelsestype/{ytelsestype}/fagsak/{eksternFagsakId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Henter forvaltningsinformasjon",
        AuditLoggerEvent.NONE,
        HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID
    )
    fun hentForvaltningsinfo(
        @PathVariable ytelsestype: Ytelsestype,
        @PathVariable eksternFagsakId: String
    ): Ressurs<Forvaltningsinfo> {
        return Ressurs.success(forvaltningService.hentForvaltningsinfo(ytelsestype, eksternFagsakId))
    }

    @Operation(summary = "Deaktiver koplet kravgrunnlag (ved feilsituasjonen når 2 aktive kravgrunnlag er koplet mot behandling)")
    @PutMapping(
        path = ["/deaktiver/kravgrunnlag/{behandlingId}/{kravgrunnlag431Id}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Deaktiver koplet kravgrunnlag",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID
    )
    fun deaktiverKopletKravgrunnlag(@PathVariable behandlingId: UUID, @PathVariable kravgrunnlag431Id: UUID): Ressurs<String> {
        forvaltningService.deaktiverKopletKravgrunnlag(behandlingId, kravgrunnlag431Id)
        return Ressurs.success("OK")
    }
}

data class Forvaltningsinfo(val eksternKravgrunnlagId: BigInteger, val mottattXmlId: UUID?, val eksternId: String)
