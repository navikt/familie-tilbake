package no.nav.familie.tilbake.api.e2e

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.KafkaConfig
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/autotest")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e", "local", "integrasjonstest")
class AutotestController(private val taskRepository: TaskRepository,
                         private val behandlingRepository: BehandlingRepository,
                         private val requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository,
                         private val kafkaTemplate: KafkaTemplate<String, String>) {

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

    @PutMapping(path = ["/behandling/{behandlingId}/endre/saksbehandler/{nyAnsvarligSaksbehandler}"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SYSTEM,
                        handling = "endre ansvarlig saksbehandler")
    fun endreAnsvarligSaksbehandler(@PathVariable behandlingId: UUID,
                                    @PathVariable nyAnsvarligSaksbehandler: String): Ressurs<String> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = nyAnsvarligSaksbehandler))
        return Ressurs.success("OK")
    }

    @PostMapping(path = ["/publiser/fagsystemsbehandling"])
    fun publishFagsystemsbehandlingsdata(@Valid @RequestBody opprettManueltTilbakekrevingRequest
                                         : OpprettManueltTilbakekrevingRequest): Ressurs<String> {
        val eksternFagsakId = opprettManueltTilbakekrevingRequest.eksternFagsakId
        val ytelsestype = opprettManueltTilbakekrevingRequest.ytelsestype
        val eksternId = opprettManueltTilbakekrevingRequest.eksternId
        val respons = HentFagsystemsbehandlingRespons(eksternFagsakId = eksternFagsakId,
                                                      ytelsestype = ytelsestype,
                                                      eksternId = eksternId,
                                                      personIdent = "testverdi",
                                                      språkkode = Språkkode.NB,
                                                      enhetId = "8020",
                                                      enhetsnavn = "testverdi",
                                                      revurderingsvedtaksdato = LocalDate.now(),
                                                      faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                                                            revurderingsresultat = "OPPHØR",
                                                                            tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                    .IGNORER_TILBAKEKREVING))
        val requestSendt = requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                                                  ytelsestype,
                                                                                                  eksternId)
        val melding = objectMapper.writeValueAsString(respons)
        val producerRecord = ProducerRecord(KafkaConfig.HENT_FAGSYSTEMSBEHANDLING_RESPONS_TOPIC,
                                            requestSendt?.id.toString(),
                                            melding)
        kafkaTemplate.send(producerRecord)
        return Ressurs.success("OK")
    }
}
