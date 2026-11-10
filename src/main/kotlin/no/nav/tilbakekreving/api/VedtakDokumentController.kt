package no.nav.tilbakekreving.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang
import no.nav.tilbakekreving.kontrakter.frontend.apis.DokumenterApi
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.vedtak.VedtakDokumentRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.math.BigInteger

@Component
@Unprotected
@Validated
class VedtakDokumentController(
    private val vedtakDokumentService: VedtakDokumentService,
    private val vedtakDokumentRepository: VedtakDokumentRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val vedtaksdokumentTilgang: VedtaksdokumentTilgang,
    private val request: HttpServletRequest,
) : DokumenterApi {
    override fun dokumenterHentVedtaksdokumenter(
        vedtakId: String,
    ): ResponseEntity<List<VedtaksdokumentDto>> {
        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
        when (vedtaksdokumentTilgang.vurder(token)) {
            VedtaksdokumentTilgang.Resultat.GODKJENT -> {}
            VedtaksdokumentTilgang.Resultat.UGYLDIG_TOKEN -> throw Feil(
                message = "Missing or invalid bearer token",
                frontendFeilmelding = "Du må være innlogget",
                httpStatus = HttpStatus.UNAUTHORIZED,
                logContext = SecureLog.Context.tom(),
            )
            VedtaksdokumentTilgang.Resultat.MANGLER_ROLLE -> throw ForbiddenError(
                message = "Klienten har ikke tilgang til vedtaksdokumenter",
                frontendFeilmelding = "Ingen tilgang til vedtaksdokumenter",
                logContext = SecureLog.Context.tom(),
            )
        }
        val id = BigInteger(vedtakId)
        val tilbakekrevingId = vedtakDokumentRepository.findTilbakekrevingIdByVedtakId(id)
        if (tilbakekrevingId != null) {
            val filter = TilbakekrevingFilter.tilbakekreving(tilbakekrevingId)
            val tilbakekreving = tilbakekrevingService.hentTilbakekreving(filter)
                ?: return ResponseEntity.ok(emptyList())

            return ResponseEntity.ok(
                tilbakekreving.brevHistorikk.alleSendteDokumenter()
                    .map { VedtaksdokumentDto(it.journalpostId, it.dokumentId) }
                    .distinct(),
            )
        }

        val dokumentreferanser = vedtakDokumentService.hentBehandlingIderGammelModell(id)
            .flatMap(vedtakDokumentService::hentDokumentreferanserGammelModell)
        return ResponseEntity.ok(dokumentreferanser.distinct())
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
