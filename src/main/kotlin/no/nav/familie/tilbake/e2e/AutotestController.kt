package no.nav.familie.tilbake.e2e

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Properties
import java.util.UUID

@RestController
@RequestMapping("/api/autotest")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e","local","integrasjonstest")
class AutotestController (private val taskRepository: TaskRepository) {

    @PostMapping(path = ["/opprett/kravgrunnlag/"])
    fun opprettKravgrunnlag(@RequestBody kravgrunnlag: String) {
        taskRepository.save(Task(type = BehandleKravgrunnlagTask.TYPE,
                                 payload = kravgrunnlag,
                                 properties = Properties().apply {
                                     this["callId"] = UUID.randomUUID()
                                 }))
    }
    @PostMapping(path = ["/opprett/statusmelding/"])
    fun opprettStatusmelding(@RequestBody statusmelding: String) {
        taskRepository.save(Task(type = BehandleStatusmeldingTask.TYPE,
                                 payload = statusmelding,
                                 properties = Properties().apply {
                                     this["callId"] = UUID.randomUUID()
                                 }))
    }
}