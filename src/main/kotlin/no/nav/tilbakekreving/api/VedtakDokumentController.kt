package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.kontrakter.frontend.apis.DokumenterApi
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
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
    private val tilgangskontrollService: TilgangskontrollService,
) : DokumenterApi {
    override fun dokumenterHentVedtaksdokumenter(
        vedtakId: String,
    ): ResponseEntity<List<VedtaksdokumentDto>> {
        val id = BigInteger(vedtakId)
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
            .flatMap { behandlingId ->
                tilgangskontrollService.validerTilgangBehandlingID(
                    behandlingId = behandlingId,
                    minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                    auditLoggerEvent = AuditLoggerEvent.ACCESS,
                    handling = "Henter dokumentreferanser for iverksatt vedtak",
                )
                vedtakDokumentService.hentDokumentreferanserGammelModell(behandlingId)
            }
        return ResponseEntity.ok(dokumentreferanser.distinct())
    }
}
