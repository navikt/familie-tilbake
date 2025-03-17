package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class IverksettVedtakssteg(
    private val behandlingsvedtakService: BehandlingsvedtakService,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TracableTaskService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<IverksettVedtakssteg>()

    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.IVERKSETT_VEDTAK} steg")
        }

        val behandling = behandlingsvedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.UNDER_IVERKSETTING)
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
        val properties =
            Properties().apply {
                setProperty("ansvarligSaksbehandler", ContextService.hentSaksbehandler(logContext))
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            }
        taskService.save(
            Task(
                type = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            ),
            logContext,
        )
    }

    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.IVERKSETT_VEDTAK} steg og behandler automatisk..")
        }
        utførSteg(behandlingId, logContext)
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK
}
