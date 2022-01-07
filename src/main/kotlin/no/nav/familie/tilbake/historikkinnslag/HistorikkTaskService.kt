package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(private val taskRepository: TaskRepository) {

    fun lagHistorikkTask(behandlingId: UUID,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         aktør: Aktør,
                         fagsystem: String,
                         triggerTid: LocalDateTime? = null,
                         beskrivelse: String? = null) {

        val properties = Properties().apply {
            setProperty("historikkinnslagstype", historikkinnslagstype.name)
            setProperty("aktør", aktør.name)
            setProperty(PropertyName.FAGSYSTEM, fagsystem)
            setProperty("opprettetTidspunkt", LocalDateTime.now().toString())
            beskrivelse?.let { setProperty("beskrivelse", it) }
        }

        val task = Task(type = LagHistorikkinnslagTask.TYPE,
                        payload = behandlingId.toString(),
                        properties = properties)
        triggerTid?.let { taskRepository.save(task.medTriggerTid(triggerTid)) } ?: taskRepository.save(task)
    }
}
