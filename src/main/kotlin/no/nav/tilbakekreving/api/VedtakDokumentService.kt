package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.tilbakekreving.vedtak.VedtakDokumentRepository
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class VedtakDokumentService(
    private val vedtakDokumentRepository: VedtakDokumentRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val brevsporingRepository: BrevsporingRepository,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    fun hentTilbakekrevingIdNyModell(vedtakId: BigInteger): String? =
        vedtakDokumentRepository.findTilbakekrevingIdByVedtakId(vedtakId)

    fun hentDokumentreferanserGammelModell(
        behandlingId: UUID,
    ): List<VedtakDokumentreferanseDto> {
        val behandling = behandlingRepository.findById(behandlingId).orElse(null) ?: return emptyList()
        val fagsak = fagsakRepository.findById(behandling.fagsakId).orElse(null) ?: return emptyList()
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = fagsak.fagsystem.tilDTO(),
            eksternFagsakId = fagsak.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter dokumentreferanser for iverksatt vedtak",
        )
        return brevsporingRepository.findAllByBehandlingId(behandlingId)
            .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
    }

    fun hentBehandlingIderGammelModell(vedtakId: BigInteger): List<UUID> =
        vedtakDokumentRepository.findBehandlingIdsByVedtakId(vedtakId)
}
