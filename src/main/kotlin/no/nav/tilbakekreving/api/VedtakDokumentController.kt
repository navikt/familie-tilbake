package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.kontrakter.frontend.apis.DokumenterApi
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.math.BigInteger

@Component
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakDokumentController(
    private val vedtakDokumentService: VedtakDokumentService,
    private val tilbakekrevingService: TilbakekrevingService,
) : DokumenterApi {
    override fun dokumenterHentVedtaksdokumenter(
        vedtakId: String,
    ): ResponseEntity<List<VedtaksdokumentDto>> {
        val id = vedtakId.tilBigInteger()
        val tilbakekrevingId = vedtakDokumentService.hentTilbakekrevingIdNyModell(id)
        if (tilbakekrevingId != null) {
            val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
                filter = TilbakekrevingFilter.tilbakekreving(tilbakekrevingId),
                valideringContext = ValideringContext.ListJournalposter,
            ) ?: return ResponseEntity.ok(emptyList())

            return ResponseEntity.ok(
                tilbakekreving.brevHistorikk.alleSendteDokumenter()
                    .map { VedtaksdokumentDto(it.journalpostId, it.dokumentId) }
                    .distinct(),
            )
        }

        val dokumentreferanser = vedtakDokumentService.hentBehandlingIderGammelModell(id)
            .flatMap(vedtakDokumentService::hentDokumentreferanserGammelModell)
        return ResponseEntity.ok(
            dokumentreferanser.distinct()
                .map { VedtaksdokumentDto(it.journalpostId, it.dokumentInfoId) },
        )
    }
}

private fun String.tilBigInteger(): BigInteger {
    if (length > 64 || !matches(Regex("[0-9]+"))) {
        throw Feil(
            message = "VedtakId må være en tallstreng med maksimalt 64 tegn",
            logContext = SecureLog.Context.tom(),
            httpStatus = HttpStatus.BAD_REQUEST,
        )
    }
    return BigInteger(this).also {
        if (it > BigInteger.valueOf(Long.MAX_VALUE)) {
            throw Feil(
                message = "VedtakId er utenfor gyldig område",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
