package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.UTFØRT
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Brevmottakersteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val oppgaveTaskService: OppgaveTaskService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Brevmottakersteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.BREVMOTTAKER} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.BREVMOTTAKER,
                AUTOUTFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.BREVMOTTAKER} steg")
        }
        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId, logContext)
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, UTFØRT),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.BREVMOTTAKER} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.BREVMOTTAKER,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.BREVMOTTAKER
}
