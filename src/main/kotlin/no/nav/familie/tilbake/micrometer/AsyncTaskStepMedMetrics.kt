package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

abstract class AsyncTaskStepMedMetrics : AsyncTaskStep {
    @Autowired
    private lateinit var taskService: TaskService

    protected abstract fun doTaskWithMetrics(task: Task)

    @Transactional
    override fun doTask(task: Task) {
        doTaskWithMetrics(task)
        registrerMetrikk(task)
    }

    private fun registrerMetrikk(task: Task) {
        if (!::taskService.isInitialized) return
        Timer
            .builder("task_tid_til_ferdigstilling")
            .tag("type", task.type)
            .tag("antall_feil", findBucket(taskService.antallFeil(task.id)))
            .serviceLevelObjectives(*SLO_BUCKETS)
            .register(Metrics.globalRegistry)
            .record(Duration.between(task.opprettetTid, LocalDateTime.now()))
    }

    companion object {
        private val SLO_BUCKETS =
            arrayOf(
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofMinutes(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofMinutes(30),
                Duration.ofHours(1),
                Duration.ofHours(3),
                Duration.ofHours(12),
                Duration.ofDays(1),
                Duration.ofDays(3),
                Duration.ofDays(7),
                Duration.ofDays(30),
            )

        internal fun findBucket(antallFeil: Int): String =
            when {
                antallFeil < 5 -> antallFeil.toString()
                antallFeil < 10 -> "5-9"
                antallFeil < 25 -> "10-24"
                else -> "25+"
            }
    }
}
