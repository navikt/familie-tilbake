package no.nav.familie.tilbake.api.e2e

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Properties
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/autotest")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e", "local", "integrasjonstest")
class AutotestController(private val taskRepository: TaskRepository,
                         private val behandlingRepository: BehandlingRepository) {

    @PostMapping(path = ["/opprett/kravgrunnlag/"])
    fun opprettKravgrunnlag(@RequestBody kravgrunnlag: String): Ressurs<String> {
        taskRepository.save(Task(type = BehandleKravgrunnlagTask.TYPE,
                                 payload = kravgrunnlag,
                                 properties = Properties().apply {
                                     this["callId"] = UUID.randomUUID()
                                 }))
        return Ressurs.success("OK")
    }

    @PostMapping(path = ["/opprett/statusmelding/"])
    fun opprettStatusmelding(@RequestBody statusmelding: String): Ressurs<String> {
        taskRepository.save(Task(type = BehandleStatusmeldingTask.TYPE,
                                 payload = statusmelding,
                                 properties = Properties().apply {
                                     this["callId"] = UUID.randomUUID()
                                 }))
        return Ressurs.success("OK")
    }

    @PutMapping(path = ["/behandling/{behandlingId}/endre/saksbehandler/{nySaksbehandlerIdent}"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SYSTEM,
                        handling = "endre ansvarlig saksbehandler")
    fun endreAnsvarligSaksbehandler(@PathVariable("behandlingId") behandlingId: UUID,
                                    @PathVariable("nySaksbehandlerIdent") ansvarligSaksbehandler: String): Ressurs<String> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = ansvarligSaksbehandler))
        return Ressurs.success("OK")
    }
}
