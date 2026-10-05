package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigInteger

private data class VedtakTreff(
    val iverksattVedtak: IverksattVedtak,
    val fagsak: Fagsak? = null,
)

@Service
class VedtakDokumentService(
    private val iverksettRepository: IverksettRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val brevsporingRepository: BrevsporingRepository,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    fun hentDokumentreferanser(
        skyldner: String,
        vedtakId: BigInteger,
    ): List<VedtakDokumentreferanseDto> {
        val treff = iverksettRepository.findByVedtakId(vedtakId)
            .mapNotNull { finnTreff(it, skyldner) }

        val treffPåVedtak = when (treff.size) {
            0 -> return emptyList()
            1 -> treff.single()
            else ->
                throw Feil(
                    message = "Fant flere behandlinger for vedtakId",
                    logContext = SecureLog.Context.tom(),
                    httpStatus = HttpStatus.CONFLICT,
                )
        }

        if (treffPåVedtak.iverksattVedtak.nyModell) {
            val autorisertTilbakekreving = tilbakekrevingService.lesTilbakekreving(
                filter = TilbakekrevingFilter.behandling(treffPåVedtak.iverksattVedtak.behandlingId),
                valideringContext = ValideringContext.ListJournalposter,
            ) ?: return emptyList()

            return autorisertTilbakekreving.brevHistorikk.alleSendteDokumenter()
                .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
        }

        val fagsak = requireNotNull(treffPåVedtak.fagsak)
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = fagsak.fagsystem.tilDTO(),
            eksternFagsakId = fagsak.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter dokumentreferanser for iverksatt vedtak",
        )
        return brevsporingRepository.findAllByBehandlingIdIn(listOf(treffPåVedtak.iverksattVedtak.behandlingId))
            .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
    }

    private fun finnTreff(
        iverksattVedtak: IverksattVedtak,
        skyldner: String,
    ): VedtakTreff? {
        if (iverksattVedtak.nyModell) {
            val tilbakekreving = tilbakekrevingService.hentTilbakekreving(
                filter = TilbakekrevingFilter.behandling(iverksattVedtak.behandlingId),
                validerScope = false,
            ) ?: return null
            if (tilbakekreving.bruker?.hentBrukerinfo()?.ident != skyldner) {
                return null
            }
            return VedtakTreff(iverksattVedtak)
        }

        val behandling = behandlingRepository.findById(iverksattVedtak.behandlingId).orElse(null) ?: return null
        val fagsak = fagsakRepository.findById(behandling.fagsakId).orElse(null) ?: return null
        if (fagsak.bruker.ident != skyldner) {
            return null
        }
        return VedtakTreff(iverksattVedtak, fagsak)
    }
}
