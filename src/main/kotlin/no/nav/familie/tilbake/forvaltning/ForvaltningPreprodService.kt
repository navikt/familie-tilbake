package no.nav.familie.tilbake.forvaltning

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Profile("!prod")
class ForvaltningPreprodService(
    private val taskService: TaskService,
    private val environment: Environment,
) {
    @Transactional
    fun leggInnTestKravgrunnlag(kravgrunnlag: String) {
        if (environment.activeProfiles.contains("prod")){
                throw IllegalStateException("Kan ikke kjøre denne tjenesten i prod")
            }
        taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = kravgrunnlag,
                properties =
                    Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
            ),
        )
    }
}
