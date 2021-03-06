package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(private val taskRepository: TaskRepository) {

    fun lagHistorikkTask(behandlingId: UUID,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         aktør: Aktør,
                         triggerTid: LocalDateTime? = null,
                         begrunnelse: String? = null) {

        val properties = Properties()
        properties.setProperty("historikkinnslagstype", historikkinnslagstype.name)
        properties.setProperty("aktor", aktør.name)
        properties.setProperty("opprettetTidspunkt", LocalDateTime.now().toString())
        begrunnelse?.let { properties.setProperty("begrunnelse", begrunnelse) }

        val task = Task(type = LagHistorikkinnslagTask.TYPE,
                        payload = behandlingId.toString(),
                        properties = properties)
        triggerTid?.let { taskRepository.save(task.medTriggerTid(triggerTid)) } ?: taskRepository.save(task)
    }
}
