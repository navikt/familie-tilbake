package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(private val taskRepository: TaskRepository) {

    fun lagHistorikkTask(behandlingId: UUID,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         aktør: Aktør) {
        val properties = Properties()
        properties.setProperty("historikkinnslagType", historikkinnslagstype.name)
        properties.setProperty("aktor", aktør.name)

        taskRepository.save(Task(type = LagHistorikkinnslagTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }
}
