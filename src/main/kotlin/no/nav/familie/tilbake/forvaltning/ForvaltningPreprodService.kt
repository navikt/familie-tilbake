package no.nav.familie.tilbake.forvaltning

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
@Profile("!prod")
class ForvaltningPreprodService(
    private val taskService: TracableTaskService,
    private val environment: Environment,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
) {
    @Transactional
    fun leggInnTestKravgrunnlag(kravgrunnlag: String) {
        if (environment.activeProfiles.contains("prod")) {
            throw IllegalStateException("Kan ikke kjøre denne tjenesten i prod")
        }
        val logContext = KravgrunnlagUtil.kravgrunnlagLogContext(kravgrunnlag)
        taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = kravgrunnlag,
                properties =
                    Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
            ),
            logContext,
        )
    }

    fun validerKravgrunnlagOgBehandling(
        behandlingId: UUID,
        kravgrunnlag: String,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val eksternBehandlingIdNode = "<urn:referanse>${behandling.aktivFagsystemsbehandling.eksternId}</urn:referanse>"
        if (!kravgrunnlag.contains(eksternBehandlingIdNode)) {
            throw Feil(
                message = "Finner ikke ekstern behandlingId fra vedtaksløsning i kravgrunnlag (referanse)",
                logContext = logContext,
            )
        }
        val eksternFagsakIdNode = "<urn:fagsystemId>${fagsak.eksternFagsakId}</urn:fagsystemId>"
        if (!kravgrunnlag.contains(eksternFagsakIdNode)) {
            throw Feil(
                message = "Finner ikke ekstern fagsakId fra vedtaksløsning i kravgrunnlag (fagsystemId)",
                logContext = logContext,
            )
        }
    }
}
