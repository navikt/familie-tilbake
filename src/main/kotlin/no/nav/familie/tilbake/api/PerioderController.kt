package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.PeriodeService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
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
    private val vedtaksbrevService: VedtaksbrevService,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
    private val fagsakRepository: FagsakRepository,
    private val periodeService: PeriodeService,
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
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandlingId)

        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)

        if (!erEnsligForsørgerOgPerioderLike && vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = false))
        }
        if (vedtaksbrevsoppsummering == null) {
            vedtaksbrevsoppsummeringRepository.insert(Vedtaksbrevsoppsummering(UUID.randomUUID(), behandlingId, null, erEnsligForsørgerOgPerioderLike))
        }

        return Ressurs.success(
            erEnsligForsørgerOgPerioderLike,
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
        val behandling = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        if (behandling.ytelsestype.tilTema() != Tema.ENF) {
            throw Exception("Kan ikke slå sammen perioder i behandling som ikke er for en enslig forsørger ytelse")
        }
        vedtaksbrevService.oppdaterSkalSammenslåPerioder(behandlingId, skalSammenslåPerioder)
        return Ressurs.success(true)
    }
}
