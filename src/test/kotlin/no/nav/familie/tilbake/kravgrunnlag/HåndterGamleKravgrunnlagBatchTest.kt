package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.batch.GammelKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HentFagsystemsbehandlingTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagBatch
import no.nav.familie.tilbake.log.SecureLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class HåndterGamleKravgrunnlagBatchTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TracableTaskService

    @Autowired
    private lateinit var håndterGamleKravgrunnlagBatch: HåndterGamleKravgrunnlagBatch

    @Test
    fun `utfør skal ikke opprette tasker når det ikke finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        // Arrange
        val mottattXml = mottattXmlRepository.insert(Testdata.getøkonomiXmlMottatt())

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        taskService.findAll().forNone { it.payload shouldBe mottattXml.id.toString() }
    }

    @Test
    fun `utfør skal ikke opprette tasker når det allerede finnes en feilet task på det samme kravgrunnlag`() {
        // Arrange
        val mottattXml = mottattXmlRepository.insert(Testdata.getøkonomiXmlMottatt())
        val task = taskService.save(Task(type = GammelKravgrunnlagTask.TYPE, payload = mottattXml.id.toString()), SecureLog.Context.tom())
        taskService.save(taskService.findById(task.id).copy(status = Status.FEILET), SecureLog.Context.tom())

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        taskService.findAll().forNone {
            it.type shouldBe HentFagsystemsbehandlingTask.TYPE
            it.payload shouldBe mottattXml.id.toString()
        }
    }

    @Test
    fun `utfør skal opprette tasker når det finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        // Arrange
        val førsteXml =
            Testdata.getøkonomiXmlMottatt().copy(
                id = UUID.randomUUID(),
                eksternFagsakId = UUID.randomUUID().toString(),
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(førsteXml)

        val andreXml =
            Testdata.getøkonomiXmlMottatt().copy(
                id = UUID.randomUUID(),
                eksternFagsakId = UUID.randomUUID().toString(),
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
                ytelsestype = Ytelsestype.SKOLEPENGER,
            )
        mottattXmlRepository.insert(andreXml)

        val tredjeXml = Testdata.getøkonomiXmlMottatt()
        mottattXmlRepository.insert(tredjeXml)

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        taskService.findAll().forExactly(2) {
            it.type shouldBe HentFagsystemsbehandlingTask.TYPE
            it.payload shouldBeIn setOf(førsteXml.id.toString(), andreXml.id.toString())
        }
    }

    @Test
    fun `utfør skal opprette tasker av type HentFagsystemsbehandlingTask med spredt TriggerTid når flere kravgrunnlagene tilhører samme ekstern fagsak id`() {
        // Arrange
        val uniqueEksternFagsakId = UUID.randomUUID().toString()
        val førsteXml =
            Testdata.getøkonomiXmlMottatt().copy(
                id = UUID.randomUUID(),
                eksternFagsakId = uniqueEksternFagsakId,
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(førsteXml)

        val andreXml =
            Testdata.getøkonomiXmlMottatt().copy(
                id = UUID.randomUUID(),
                eksternFagsakId = uniqueEksternFagsakId,
                sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            )
        mottattXmlRepository.insert(andreXml)

        // Act
        håndterGamleKravgrunnlagBatch.utfør()

        // Assert
        val hentFagsystemsbehandlingTasksSortertPåTriggerTid =
            taskService.findAll()
                .filter { it.type == HentFagsystemsbehandlingTask.TYPE && it.payload in setOf(førsteXml.id.toString(), andreXml.id.toString()) }
                .sortedBy { it.triggerTid }
        assertThat(hentFagsystemsbehandlingTasksSortertPåTriggerTid).hasSize(2)
        val hentFagsystemsbehandlingTask1 = hentFagsystemsbehandlingTasksSortertPåTriggerTid[0]
        val hentFagsystemsbehandlingTask2 = hentFagsystemsbehandlingTasksSortertPåTriggerTid[1]
        assertThat(hentFagsystemsbehandlingTask1.triggerTid.plusHours(1)).isBetween(
            hentFagsystemsbehandlingTask2.triggerTid.minusMinutes(1),
            hentFagsystemsbehandlingTask2.triggerTid.plusMinutes(1),
        )
    }
}
