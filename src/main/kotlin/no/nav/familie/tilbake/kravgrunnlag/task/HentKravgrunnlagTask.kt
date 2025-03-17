package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentKravgrunnlagTask.TYPE,
    beskrivelse = "Henter kravgrunnlag fra økonomi",
    triggerTidVedFeilISekunder = 300L,
)
class HentKravgrunnlagTask(
    private val behandlingRepository: BehandlingRepository,
    private val hentKravgrunnlagService: HentKravgrunnlagService,
    private val stegService: StegService,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<HentKravgrunnlagService>()

    @Transactional
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("HentKravgrunnlagTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.type != Behandlingstype.REVURDERING_TILBAKEKREVING) {
            throw Feil(
                message = "HentKravgrunnlagTask kan kjøres bare for tilbakekrevingsrevurdering.",
                logContext = logContext,
            )
        }
        val originalBehandlingId = requireNotNull(behandling.sisteÅrsak?.originalBehandlingId)

        val tilbakekrevingsgrunnlag = hentKravgrunnlagService.hentTilbakekrevingskravgrunnlag(originalBehandlingId)
        val hentetKravgrunnlag =
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                tilbakekrevingsgrunnlag.eksternKravgrunnlagId,
                KodeAksjon.HENT_GRUNNLAG_OMGJØRING,
                logContext,
            )
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag, logContext)

        hentKravgrunnlagService.opprettHistorikkinnslag(behandlingId, logContext)

        stegService.håndterSteg(behandlingId, logContext)
    }

    companion object {
        const val TYPE = "hentKravgrunnlag"
    }
}
