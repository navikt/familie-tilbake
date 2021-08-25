package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagBatch
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGammelKravgrunnlagTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class HåndterGamleKravgrunnlagBatchTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var håndterGamleKravgrunnlagBatch: HåndterGamleKravgrunnlagBatch


    @Test
    fun `utfør skal ikke opprette tasker når det ikke finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)

        assertDoesNotThrow { håndterGamleKravgrunnlagBatch.utfør() }
        assertTrue { (taskRepository.findAll() as List<*>).isEmpty() }
    }

    @Test
    fun `utfør skal opprette tasker når det finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        val førsteXml = Testdata.økonomiXmlMottatt.copy(id = UUID.randomUUID(),
                                                        sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)))
        mottattXmlRepository.insert(førsteXml)

        val andreXml = Testdata.økonomiXmlMottatt.copy(id = UUID.randomUUID(),
                                                       sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(7)),
                                                       ytelsestype = Ytelsestype.SKOLEPENGER)
        mottattXmlRepository.insert(andreXml)

        val tredjeXml = Testdata.økonomiXmlMottatt
        mottattXmlRepository.insert(tredjeXml)

        assertDoesNotThrow { håndterGamleKravgrunnlagBatch.utfør() }
        assertTrue { (taskRepository.findAll() as List<*>).isNotEmpty() }
        assertTrue { taskRepository.findAll().count { it.type == HåndterGammelKravgrunnlagTask.TYPE } == 2}
    }
}