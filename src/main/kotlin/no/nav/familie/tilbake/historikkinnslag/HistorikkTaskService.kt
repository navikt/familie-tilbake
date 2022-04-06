package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(private val taskRepository: TaskRepository,
                           private val fagsakService: FagsakService) {

    fun lagHistorikkTask(behandlingId: UUID,
                         historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                         aktør: Aktør,
                         triggerTid: LocalDateTime? = null,
                         beskrivelse: String? = null,
                         ukjentAdresse: Boolean? = false) {

        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties = Properties().apply {
            setProperty("historikkinnslagstype", historikkinnslagstype.name)
            setProperty("aktør", aktør.name)
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty("opprettetTidspunkt", LocalDateTime.now().toString())
            if (ukjentAdresse != null && ukjentAdresse) {
                setProperty("beskrivelse", "Mottaker har ukjent adresse, brev ikke sendt")
            } else {
                beskrivelse?.let { setProperty("beskrivelse", fjernNewlinesFraString(it)) }
            }
        }

        val task = Task(type = LagHistorikkinnslagTask.TYPE,
                        payload = behandlingId.toString(),
                        properties = properties)
        triggerTid?.let { taskRepository.save(task.medTriggerTid(triggerTid)) } ?: taskRepository.save(task)
    }

    private fun fjernNewlinesFraString(tekst: String): String {
        return tekst
                .replace("\r", "")
                .replace("\n", " ")
    }
}
