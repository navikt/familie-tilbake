package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigInteger

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
        vedtakId: BigInteger,
    ): List<VedtakDokumentreferanseDto> {
        val treff = iverksettRepository.findByVedtakId(vedtakId)

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

        if (treffPåVedtak.nyModell) {
            val autorisertTilbakekreving = tilbakekrevingService.lesTilbakekreving(
                filter = TilbakekrevingFilter.behandling(treffPåVedtak.behandlingId),
                valideringContext = ValideringContext.ListJournalposter,
            ) ?: return emptyList()

            return autorisertTilbakekreving.brevHistorikk.alleSendteDokumenter()
                .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
        }

        val behandling = behandlingRepository.findById(treffPåVedtak.behandlingId).orElse(null) ?: return emptyList()
        val fagsak = fagsakRepository.findById(behandling.fagsakId).orElse(null) ?: return emptyList()
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = fagsak.fagsystem.tilDTO(),
            eksternFagsakId = fagsak.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter dokumentreferanser for iverksatt vedtak",
        )
        return brevsporingRepository.findAllByBehandlingId(treffPåVedtak.behandlingId)
            .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
    }
}
