package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Varselssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Varselssteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.VARSEL} steg")
        }
        log.medContext(logContext) {
            info(
                "Behandling $behandlingId venter på ${Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING}. " +
                    "Den kan kun tas av vent av saksbehandler ved å gjenoppta behandlingen",
            )
        }
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.VARSEL} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.VARSEL,
                Behandlingsstegstatus.UTFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.VARSEL
}
