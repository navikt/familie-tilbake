package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/perioder")
@ProtectedWithClaims(issuer = "azuread")
class PerioderController(
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val vilkårsVurderingService: VilkårsvurderingService,
    private val foreldelseService: ForeldelseService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
    ): Ressurs<Boolean> {
        val erPerioderLike =
            faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
                foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId) &&
                vilkårsVurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId)

        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        if (!erPerioderLike && vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = false))
        }
        return Ressurs.success(
            erPerioderLike,
        )
    }

    @Operation(summary = "Skal oppdatere skalSammenslåPerioder")
    @PostMapping(
        "/slaa-sammen-perioder/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Skal oppdatere skalSammenslåPerioder",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun slåSammenPerioder(
        @PathVariable behandlingId: UUID,
        @RequestParam("skalSammenslaa") skalSammenslåPerioder: Boolean,
    ): Ressurs<Boolean> {
        vedtaksbrevService.oppdaterSkalSammenslåPerioder(behandlingId, skalSammenslåPerioder)
        return Ressurs.success(true)
    }

    @Operation(summary = "Er perioder slått sammen")
    @GetMapping(
        "/erPerioderSlaattSammen/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Er perioder slått sammen",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun erPerioderSlåttSammen(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        val erPerioderSlåttSammen =
            vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)?.skalSammenslåPerioder
                ?: faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
                foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId) &&
                vilkårsVurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId)
        return Ressurs.success(erPerioderSlåttSammen)
    }
}
