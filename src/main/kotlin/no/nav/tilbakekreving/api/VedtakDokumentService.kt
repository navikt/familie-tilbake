package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.vedtak.IverksettRepository
import no.nav.tilbakekreving.vedtak.IverksettelseReferanse
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class VedtakDokumentService(
    private val iverksettRepository: IverksettRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val brevsporingRepository: BrevsporingRepository,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    fun hentIverksettelser(
        vedtakId: BigInteger,
    ): List<IverksettelseReferanse> =
        iverksettRepository.findByVedtakId(vedtakId)
            .distinctBy { it.nyModell to it.behandlingId }

    fun hentDokumentreferanserNyModell(
        behandlingId: UUID,
    ): List<VedtakDokumentreferanseDto> {
        val autorisertTilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.ListJournalposter,
        ) ?: return emptyList()

        return autorisertTilbakekreving.brevHistorikk.alleSendteDokumenter()
            .map { VedtakDokumentreferanseDto(it.journalpostId, it.dokumentId) }
    }

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
}
