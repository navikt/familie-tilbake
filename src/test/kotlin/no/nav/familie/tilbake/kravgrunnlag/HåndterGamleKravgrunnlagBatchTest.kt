package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.avstemming.task.AvstemmingTask
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.batch.GammelKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HentFagsystemsbehandlingTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class HåndterGamleKravgrunnlagBatchTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var håndterGamleKravgrunnlagBatch: HåndterGamleKravgrunnlagBatch

    @Test
    fun `utfør skal ikke opprette tasker når det ikke finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        // Arrange
        mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        assertThat(taskService.findAll().filter { it.type != AvstemmingTask.TYPE }).isEmpty()
    }

    @Test
    fun `utfør skal ikke opprette tasker når det allerede finnes en feilet task på det samme kravgrunnlag`() {
        // Arrange
        val mottattXml = mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)
        val task = taskService.save(Task(type = GammelKravgrunnlagTask.TYPE, payload = mottattXml.id.toString()))
        taskService.save(taskService.findById(task.id).copy(status = Status.FEILET))

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        assertThat(taskService.findAll().any { it.type == HentFagsystemsbehandlingTask.TYPE }).isFalse()
    }

    @Test
    fun `utfør skal opprette tasker når det finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        // Arrange
        val førsteXml =
            Testdata.økonomiXmlMottatt.copy(
                id = UUID.randomUUID(),
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(førsteXml)

        val andreXml =
            Testdata.økonomiXmlMottatt.copy(
                id = UUID.randomUUID(),
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
                ytelsestype = Ytelsestype.SKOLEPENGER,
            )
        mottattXmlRepository.insert(andreXml)

        val tredjeXml = Testdata.økonomiXmlMottatt
        mottattXmlRepository.insert(tredjeXml)

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        val alleTasks = taskService.findAll()
        assertThat(alleTasks).hasSizeGreaterThanOrEqualTo(2)
        assertThat(alleTasks.filter { it.type == HentFagsystemsbehandlingTask.TYPE }).hasSize(2)
    }

    @Test
    fun `utfør skal opprette tasker av type HentFagsystemsbehandlingTask med spredt TriggerTid når flere kravgrunnlagene tilhører samme ekstern fagsak id`() {
        // Arrange
        val førsteXml =
            Testdata.økonomiXmlMottatt.copy(
                id = UUID.randomUUID(),
                eksternFagsakId = "1",
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(førsteXml)

        val andreXml =
            Testdata.økonomiXmlMottatt.copy(
                id = UUID.randomUUID(),
                eksternFagsakId = "1",
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(andreXml)

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        val alleTasks = taskService.findAll()
        assertThat(alleTasks).hasSizeGreaterThanOrEqualTo(2)
        val hentFagsystemsbehandlingTasksSortertPåTriggerTid = alleTasks.filter { it.type == HentFagsystemsbehandlingTask.TYPE }.sortedBy { it.triggerTid }
        assertThat(hentFagsystemsbehandlingTasksSortertPåTriggerTid).hasSize(2)
        val hentFagsystemsbehandlingTask1 = hentFagsystemsbehandlingTasksSortertPåTriggerTid[0]
        val hentFagsystemsbehandlingTask2 = hentFagsystemsbehandlingTasksSortertPåTriggerTid[1]
        assertThat(hentFagsystemsbehandlingTask1.triggerTid.plusHours(1)).isBetween(
            hentFagsystemsbehandlingTask2.triggerTid.minusMinutes(1),
            hentFagsystemsbehandlingTask2.triggerTid.plusMinutes(1),
        )
    }
}
