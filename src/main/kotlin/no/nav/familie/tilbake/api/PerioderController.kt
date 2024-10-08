package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.PeriodeService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
    ): Ressurs<Boolean> {
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandlingId)
        return Ressurs.success(
            erEnsligForsørgerOgPerioderLike,
        )
    }

    @Operation(summary = "Oppdatere skalSammenslåPerioder")
    @PostMapping(
        "/sammensla/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Oppdatere skalSammenslåPerioder",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun sammenslå(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        val behandling = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        if (behandling.ytelsestype.tilTema() != Tema.ENF) {
            throw Exception("Kan ikke slå sammen perioder i behandling som ikke er for en enslig forsørger ytelse")
        }
        vedtaksbrevService.oppdaterSkalSammenslåPerioder(behandlingId, true)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Angre sammenslå av perioder")
    @PostMapping(
        "/angre-sammenslaing/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.SAKSBEHANDLER,
        "Angre sammenslåing av perioder",
        AuditLoggerEvent.UPDATE,
        HenteParam.BEHANDLING_ID,
    )
    fun angreSammenslåing(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        if (vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = false))
        }
        return Ressurs.success("OK")
    }
}
