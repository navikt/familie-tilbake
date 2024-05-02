package no.nav.familie.tilbake.forvaltning

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
@Profile("!prod")
class ForvaltningPreprodService(
    private val taskService: TaskService,
    private val environment: Environment,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
) {
    @Transactional
    fun leggInnTestKravgrunnlag(kravgrunnlag: String) {
        if (environment.activeProfiles.contains("prod")) {
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

    fun validerKravgrunnlagOgBehandling(
        behandlingId: UUID,
        kravgrunnlag: String,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (!kravgrunnlag.contains(behandling.aktivFagsystemsbehandling.eksternId)) {
            throw Feil("Finner ikke ekstern behandlingId i kravgrunnlag")
        }

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        if (!kravgrunnlag.contains(fagsak.eksternFagsakId)) {
            throw Feil("Finner ikke ekstern fagsakId i kravgrunnlag")
        }
    }
}
