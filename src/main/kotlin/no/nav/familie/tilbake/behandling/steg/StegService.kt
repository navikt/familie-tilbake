package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    val steg: List<IBehandlingssteg>,
    val behandlingRepository: BehandlingRepository,
    val behandlingskontrollService: BehandlingskontrollService,
    val validerBrevmottakerService: ValiderBrevmottakerService,
) {
    @Transactional
    fun håndterSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        var aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)

        hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId, logContext)

        // Autoutfør brevmottaker steg og verge steg om verge informasjon er kopiert fra fagsystem
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)
        when (aktivtBehandlingssteg) {
            Behandlingssteg.BREVMOTTAKER, Behandlingssteg.VERGE -> hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId, logContext)
            else -> return
        }
    }

    @Transactional
    fun håndterSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil(
                message = "Behandling med id=$behandlingId er allerede ferdig behandlet",
                logContext = logContext,
            )
        }
        val behandledeSteg: Behandlingssteg = Behandlingssteg.fraNavn(behandlingsstegDto.getSteg())
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(
                message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                frontendFeilmelding = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        var aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)
        if (Behandlingssteg.FORESLÅ_VEDTAK == aktivtBehandlingssteg) {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId = behandling.id,
                fagsakId = behandling.fagsakId,
                logContext = logContext,
            )
        }
        // Behandling kan ikke tilbakeføres når er på FatteVedtak/IverksetteVedtak steg
        if (Behandlingssteg.FATTE_VEDTAK == aktivtBehandlingssteg || Behandlingssteg.IVERKSETT_VEDTAK == aktivtBehandlingssteg) {
            if (behandlingsstegDto is BehandlingsstegFatteVedtaksstegDto) {
                hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto, logContext)

                aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)
                if (aktivtBehandlingssteg == Behandlingssteg.IVERKSETT_VEDTAK) {
                    hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId, logContext)
                }
            }
            return
        }
        behandlingskontrollService.behandleStegPåNytt(behandlingId, behandledeSteg, logContext)
        hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto, logContext)

        // sjekk om aktivtBehandlingssteg kan autoutføres
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)
        if (aktivtBehandlingssteg in
            listOf(
                Behandlingssteg.FORELDELSE,
                Behandlingssteg.VILKÅRSVURDERING,
            )
        ) {
            hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId, logContext)
        }
    }

    @Transactional
    fun håndterStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)

        håndterStegAutomatisk(behandling, aktivtBehandlingssteg, logContext)
    }

    @Transactional
    fun håndterStegAutomatisk(
        behandling: Behandling,
        aktivtBehandlingssteg: Behandlingssteg,
        logContext: SecureLog.Context,
    ) {
        validerAtBehandlingIkkeErAvsluttet(behandling, logContext)
        validerAtUtomatiskBehandlingIkkeErEøs(behandling, logContext)
        validerAtBehandlingIkkeErPåVent(
            behandlingId = behandling.id,
            erBehandlingPåVent = behandlingskontrollService.erBehandlingPåVent(behandling.id),
            behandledeSteg = aktivtBehandlingssteg.name,
            logContext = logContext,
        )
        validerAtBehandlingErAutomatisk(behandling, logContext)

        if (aktivtBehandlingssteg != Behandlingssteg.AVSLUTTET) {
            hentStegInstans(aktivtBehandlingssteg).utførStegAutomatisk(behandling.id, logContext)

            if (aktivtBehandlingssteg != Behandlingssteg.IVERKSETT_VEDTAK) {
                val nesteSteg = hentAktivBehandlingssteg(behandling.id, logContext)
                håndterStegAutomatisk(behandling, nesteSteg, logContext)
            }
        }
    }

    @Transactional
    fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        var aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)

        hentStegInstans(aktivtBehandlingssteg).gjenopptaSteg(behandlingId, logContext)

        // Autoutfør brevmottaker steg og verge steg om verge informasjon er kopiert fra fagsystem
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId, logContext)
        when (aktivtBehandlingssteg) {
            Behandlingssteg.BREVMOTTAKER, Behandlingssteg.VERGE -> hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId, logContext)
            else -> return
        }
    }

    @Transactional
    fun angreSendTilBeslutter(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandling.id)

        if (behandlingsstegstilstand?.behandlingssteg != Behandlingssteg.FATTE_VEDTAK) {
            throw Feil(
                message = "Kan ikke angre send til beslutter når behandlingen er i steg ${behandlingsstegstilstand?.behandlingssteg}",
                logContext = logContext,
            )
        }

        if (behandling.status != Behandlingsstatus.FATTER_VEDTAK) {
            throw Feil(
                message = "Kan ikke angre send til beslutter når behandlingen har status ${behandling.status}",
                logContext = logContext,
            )
        }

        behandlingskontrollService.behandleStegPåNytt(behandling.id, Behandlingssteg.FORESLÅ_VEDTAK, logContext)
    }

    fun kanAnsvarligSaksbehandlerOppdateres(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
    ): Boolean {
        val behandlingssteg = Behandlingssteg.fraNavn(behandlingsstegDto.getSteg())
        return when (behandlingssteg) {
            Behandlingssteg.IVERKSETT_VEDTAK, Behandlingssteg.FATTE_VEDTAK -> false
            else -> true
        }
    }

    private fun hentAktivBehandlingssteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Behandlingssteg {
        val aktivtBehandlingssteg =
            behandlingskontrollService.finnAktivtSteg(behandlingId)
                ?: throw Feil(
                    message = "Behandling $behandlingId har ikke noe aktiv steg",
                    frontendFeilmelding = "Behandling $behandlingId har ikke noe aktiv steg",
                    logContext = logContext,
                )
        if (aktivtBehandlingssteg !in
            setOf(
                Behandlingssteg.VARSEL,
                Behandlingssteg.GRUNNLAG,
                Behandlingssteg.BREVMOTTAKER,
                Behandlingssteg.VERGE,
                Behandlingssteg.FAKTA,
                Behandlingssteg.FORELDELSE,
                Behandlingssteg.VILKÅRSVURDERING,
                Behandlingssteg.FORESLÅ_VEDTAK,
                Behandlingssteg.FATTE_VEDTAK,
                Behandlingssteg.IVERKSETT_VEDTAK,
            )
        ) {
            throw Feil(
                message = "Steg $aktivtBehandlingssteg er ikke implementer ennå",
                logContext = logContext,
            )
        }

        return aktivtBehandlingssteg
    }

    private fun hentStegInstans(behandlingssteg: Behandlingssteg): IBehandlingssteg =
        steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
            ?: error("Finner ikke behandlingssteg $behandlingssteg")
}
