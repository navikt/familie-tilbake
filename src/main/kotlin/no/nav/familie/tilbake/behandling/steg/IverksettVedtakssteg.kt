package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class IverksettVedtakssteg(private val behandlingsvedtakService: BehandlingsvedtakService,
                           private val taskService: TaskService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.IVERKSETT_VEDTAK} steg")

        behandlingsvedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.UNDER_IVERKSETTING)

        val properties = Properties()
        properties.setProperty("ansvarligSaksbehandler", ContextService.hentSaksbehandler())
        taskService.save(Task(type = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
                              payload = behandlingId.toString(),
                              properties = properties))

    }

    override fun utførStegAutomatisk(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.IVERKSETT_VEDTAK} steg og behandler automatisk..")
        utførSteg(behandlingId)
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.IVERKSETT_VEDTAK
    }
}
