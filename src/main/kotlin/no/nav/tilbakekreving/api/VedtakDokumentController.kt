package no.nav.tilbakekreving.api

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang
import no.nav.tilbakekreving.kontrakter.frontend.apis.DokumenterApi
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.vedtak.VedtakDokumentRepository
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
) : DokumenterApi {
    override fun dokumenterHentVedtaksdokumenter(
        authorization: String,
        vedtakId: String,
    ): ResponseEntity<List<VedtaksdokumentDto>> {
        val token = authorization
            .takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
        vedtaksdokumentTilgang.valider(token, VedtaksdokumentTilgang.Tilgang.TVANGSGRUNNLAG_LES)
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
