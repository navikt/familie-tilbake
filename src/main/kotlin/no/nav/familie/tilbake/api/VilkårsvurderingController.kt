package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
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
class VilkårsvurderingController(
    val vilkårsvurderingService: VilkårsvurderingService,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    @Operation(summary = "Hent vilkårsvurdering")
    @GetMapping(
        path = ["{behandlingId}/vilkarsvurdering/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentVurdertVilkårsvurdering(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<VurdertVilkårsvurderingDto> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter vilkårsvurdering for en gitt behandling",
        )
        return Ressurs.success(vilkårsvurderingService.hentVilkårsvurdering(behandlingId))
    }

    @Operation(summary = "Hent inaktive vilkårsvurderinger")
    @GetMapping(
        path = ["{behandlingId}/vilkarsvurdering/inaktiv"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentInaktivVilkårsvurdering(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<List<VurdertVilkårsvurderingDto>> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter inaktive vilkårsvurderinger for en gitt behandling",
        )
        return Ressurs.success(vilkårsvurderingService.hentInaktivVilkårsvurdering(behandlingId))
    }
}
