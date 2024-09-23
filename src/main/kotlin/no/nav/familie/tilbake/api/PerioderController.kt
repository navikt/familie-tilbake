package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/perioder")
@ProtectedWithClaims(issuer = "azuread")
class PerioderController(
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val vilkårsVurderingService: VilkårsvurderingService,
    private val foreldelseService: ForeldelseService,
) {
    @Operation(summary = "Sjekker om perioder er like - unntatt dato og beløp")
    @GetMapping(
        "/sjekk-likhet/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Sjekker om perioder er like - unntatt dato og beløp",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun erPerioderLike(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> =
        Ressurs.success(
            faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
                foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId) &&
                vilkårsVurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId),
        )
}
