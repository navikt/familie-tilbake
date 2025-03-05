package no.nav.familie.tilbake.api.e2e

import jakarta.validation.Valid
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.kontrakter.Ressurs
import no.nav.tilbakekreving.kontrakter.Språkkode
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Faktainfo
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Institusjon
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.tilbakekreving.v1.HentFagsystemsbehandling
import no.nav.tilbakekreving.kontrakter.tilbakekreving.v1.HentFagsystemsbehandlingRespons
import no.nav.tilbakekreving.kontrakter.tilbakekreving.v1.OpprettManueltTilbakekrevingRequest
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@RestController
@RequestMapping("/api/autotest")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e", "local", "integrasjonstest")
class AutotestController(
    private val taskService: TracableTaskService,
    private val behandlingRepository: BehandlingRepository,
    private val requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository,
    private val kafkaProducer: KafkaProducer,
    private val environment: Environment,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    @PostMapping(path = ["/opprett/kravgrunnlag/"])
    fun opprettKravgrunnlag(
        @RequestBody kravgrunnlag: String,
    ): Ressurs<String> {
        taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = kravgrunnlag,
                properties =
                    Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
            ),
            SecureLog.Context.tom(),
        )
        return Ressurs.success("OK")
    }

    @PostMapping(path = ["/opprett/statusmelding/"])
    fun opprettStatusmelding(
        @RequestBody statusmelding: String,
    ): Ressurs<String> {
        taskService.save(
            Task(
                type = BehandleStatusmeldingTask.TYPE,
                payload = statusmelding,
                properties =
                    Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
            ),
            SecureLog.Context.tom(),
        )
        return Ressurs.success("OK")
    }

    @PutMapping(
        path = ["/behandling/{behandlingId}/endre/saksbehandler/{nyAnsvarligSaksbehandler}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun endreAnsvarligSaksbehandler(
        @PathVariable behandlingId: UUID,
        @PathVariable nyAnsvarligSaksbehandler: String,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "endre ansvarlig saksbehandler",
        )
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(ansvarligSaksbehandler = nyAnsvarligSaksbehandler))
        return Ressurs.success("OK")
    }

    @PostMapping(path = ["/publiser/fagsystemsbehandling"])
    fun publishFagsystemsbehandlingsdata(
        @Valid @RequestBody
        opprettManueltTilbakekrevingRequest: OpprettManueltTilbakekrevingRequest,
        @RequestParam(required = false, name = "erInstitusjon") erInstitusjon: Boolean = false,
    ): Ressurs<String> {
        val eksternFagsakId = opprettManueltTilbakekrevingRequest.eksternFagsakId
        val ytelsestype = opprettManueltTilbakekrevingRequest.ytelsestype
        val eksternId = opprettManueltTilbakekrevingRequest.eksternId
        val institusjon = if (erInstitusjon) Institusjon(organisasjonsnummer = "987654321") else null
        val fagsystemsbehandling =
            HentFagsystemsbehandling(
                eksternFagsakId = eksternFagsakId,
                ytelsestype = ytelsestype,
                eksternId = eksternId,
                personIdent = "12345678901",
                språkkode = Språkkode.NB,
                enhetId = "8020",
                enhetsnavn = "testverdi",
                revurderingsvedtaksdato = LocalDate.now(),
                faktainfo =
                    Faktainfo(
                        revurderingsårsak = "testverdi",
                        revurderingsresultat = "OPPHØR",
                        tilbakekrevingsvalg =
                            Tilbakekrevingsvalg
                                .IGNORER_TILBAKEKREVING,
                    ),
                institusjon = institusjon,
            )
        val requestSendt =
            requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        val melding =
            objectMapper.writeValueAsString(HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = fagsystemsbehandling))
        if (environment.activeProfiles.any { it.contains("e2e") }) {
            requestSendtRepository.update(requestSendt!!.copy(respons = melding))
        } else {
            kafkaProducer.sendRåFagsystemsbehandlingResponse(
                requestSendt?.id,
                melding,
            )
        }
        return Ressurs.success("OK")
    }
}
