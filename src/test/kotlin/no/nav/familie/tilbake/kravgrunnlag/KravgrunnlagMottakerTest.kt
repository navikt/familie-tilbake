package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.longs.shouldBeGreaterThan
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class KravgrunnlagMottakerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    fun `verifiser at det er mulig å lagre en task`() {
        taskRepository.save(Task(type = BehandleKravgrunnlagTask.TYPE,
                                 payload = "kravgrunnlagFraOppdrag"))
        taskRepository.count() shouldBeGreaterThan 0
    }
}
