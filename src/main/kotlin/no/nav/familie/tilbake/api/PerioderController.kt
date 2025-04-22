package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.PeriodeService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/perioder")
@ProtectedWithClaims(issuer = "azuread")
class PerioderController(
    private val vedtaksbrevService: VedtaksbrevService,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
    private val fagsakRepository: FagsakRepository,
    private val periodeService: PeriodeService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Sjekker om perioder er like - unntatt dato og beløp")
    @GetMapping(
        "/sjekk-likhet/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun erPerioderLike(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Sjekker om perioder er like - unntatt dato og beløp",
            )
            tilbakekrevingService.sjekkBehovOgHåndter(tilbakekreving)
            return Ressurs.success(tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsteg.harLikePerioder())
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Sjekker om perioder er like - unntatt dato og beløp",
        )
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandlingId)
        return Ressurs.success(
            erEnsligForsørgerOgPerioderLike == SkalSammenslåPerioder.JA,
        )
    }

    @Operation(summary = "Sjekker om perioder er sammenslått")
    @GetMapping(
        "/hent-sammenslatt/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun erPerioderSammenslått(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Sjekker om perioder er sammenslått",
            )
            tilbakekrevingService.sjekkBehovOgHåndter(tilbakekreving)
            return Ressurs.success(tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsteg.harLikePerioder())
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Sjekker om perioder er sammenslått",
        )
        val erPerioderSammenslått = periodeService.erPerioderSammenslått(behandlingId)
        return Ressurs.success(
            erPerioderSammenslått,
        )
    }

    @Operation(summary = "Oppdatere skalSammenslåPerioder")
    @PostMapping(
        "/sammensla/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sammenslå(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Oppdatere skalSammenslåPerioder",
        )
        val behandling = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        if (behandling.ytelsestype.tilTema() != Tema.ENF) {
            throw Exception("Kan ikke slå sammen perioder i behandling som ikke er for en enslig forsørger ytelse")
        }
        vedtaksbrevService.oppdaterSkalSammenslåPerioder(behandlingId, SkalSammenslåPerioder.JA)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Angre sammenslå av perioder")
    @PostMapping(
        "/angre-sammenslaing/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun angreSammenslåing(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Angre sammenslåing av perioder",
        )
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        if (vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = SkalSammenslåPerioder.NEI))
        }
        return Ressurs.success("OK")
    }
}
