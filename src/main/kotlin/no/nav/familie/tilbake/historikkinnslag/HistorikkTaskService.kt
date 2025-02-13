package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(
    private val taskService: TracableTaskService,
    private val fagsakRepository: FagsakRepository,
) {
    fun lagHistorikkTask(
        behandlingId: UUID,
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        triggerTid: LocalDateTime? = null,
        beskrivelse: String? = null,
        brevtype: Brevtype? = null,
        beslutter: String? = null,
    ) {
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandlingId.toString())
        val properties =
            Properties().apply {
                setProperty("historikkinnslagstype", historikkinnslagstype.name)
                setProperty("aktør", aktør.name)
                setProperty(PropertyName.FAGSYSTEM, fagsak.fagsystem.name)
                setProperty("opprettetTidspunkt", LocalDateTime.now().toString())
                beslutter?.let { setProperty(PropertyName.BESLUTTER, beslutter) }
                beskrivelse?.let { setProperty("beskrivelse", fjernNewlinesFraString(it)) }
                brevtype?.let { setProperty("brevtype", brevtype.name) }
            }

        val task =
            Task(
                type = LagHistorikkinnslagTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            )
        triggerTid?.let { taskService.save(task.medTriggerTid(triggerTid), logContext) } ?: taskService.save(task, logContext)
    }

    private fun fjernNewlinesFraString(tekst: String): String =
        tekst
            .replace("\r", "")
            .replace("\n", " ")
}
