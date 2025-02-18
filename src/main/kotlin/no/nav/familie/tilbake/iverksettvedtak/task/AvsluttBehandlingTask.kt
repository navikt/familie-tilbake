package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AvsluttBehandlingTask.TYPE,
    beskrivelse = "Avslutter behandling",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AvsluttBehandlingTask(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkService: HistorikkService,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<AvsluttBehandlingTask>()

    @Transactional
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("AvsluttBehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        }

        var behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.status == Behandlingsstatus.AVSLUTTET) {
            log.medContext(logContext) {
                info("Behandling er allerede avsluttet")
            }
            return
        }

        if (!behandling.erUnderIverksettelse) {
            throw Feil(
                message = "Behandling med id=$behandlingId kan ikke avsluttes",
                logContext = logContext,
            )
        }

        behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                status = Behandlingsstatus.AVSLUTTET,
                avsluttetDato = LocalDate.now(),
            ),
        )

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.AVSLUTTET,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
                logContext = logContext,
            )

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_AVSLUTTET,
            aktør = Aktør.Vedtaksløsning,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    companion object {
        const val TYPE = "avsluttBehandling"
    }
}
