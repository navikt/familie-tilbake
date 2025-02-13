package no.nav.familie.tilbake.behandling.task

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TracableTaskService(
    private val taskService: TaskService,
) {
    fun save(
        task: Task,
        logContext: SecureLog.Context,
    ) = taskService.save(
        task.apply {
            logContext.copyTo(task.metadata)
        },
    )

    fun finnTasksMedStatus(
        status: List<Status>,
        type: String? = null,
        page: Pageable = Pageable.unpaged(),
    ) = taskService.finnTasksMedStatus(status)

    fun finnTasksMedStatus(
        status: List<Status>,
        page: Pageable,
    ) = taskService.finnTasksMedStatus(status, page)

    fun findById(id: Long) = taskService.findById(id)

    fun findAll() = taskService.findAll()

    fun finnAlleTaskerMedPayloadOgType(
        payload: String,
        type: String,
    ) = taskService.finnAlleTaskerMedPayloadOgType(payload, type)
}
