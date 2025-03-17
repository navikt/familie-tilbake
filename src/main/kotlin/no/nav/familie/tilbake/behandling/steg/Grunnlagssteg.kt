package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Grunnlagssteg(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkService: HistorikkService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Grunnlagssteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.GRUNNLAG} steg")
        }
        if (kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    Behandlingssteg.GRUNNLAG,
                    Behandlingsstegstatus.UTFØRT,
                ),
                logContext,
            )
            behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
            historikkService.lagHistorikkinnslag(
                behandlingId = behandlingId,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                aktør = Aktør.Vedtaksløsning,
                opprettetTidspunkt = LocalDateTime.now().plusSeconds(2),
            )
        }
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        utførSteg(behandlingId, logContext)
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.GRUNNLAG} steg")
        }
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.GRUNNLAG
}
