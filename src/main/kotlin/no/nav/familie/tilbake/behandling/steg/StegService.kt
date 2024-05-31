package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
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
    fun håndterSteg(behandlingId: UUID) {
        var aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)

        hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)

        // Autoutfør brevmottaker steg og verge steg om verge informasjon er kopiert fra fagsystem
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        when (aktivtBehandlingssteg) {
            Behandlingssteg.BREVMOTTAKER, Behandlingssteg.VERGE -> hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
            else -> return
        }
    }

    @Transactional
    fun håndterSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet")
        }
        val behandledeSteg: Behandlingssteg = Behandlingssteg.fraNavn(behandlingsstegDto.getSteg())
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(
                message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                frontendFeilmelding = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        var aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)
        if (Behandlingssteg.FORESLÅ_VEDTAK == aktivtBehandlingssteg) {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(behandlingId = behandling.id, fagsakId = behandling.fagsakId)
        }
        // Behandling kan ikke tilbakeføres når er på FatteVedtak/IverksetteVedtak steg
        if (Behandlingssteg.FATTE_VEDTAK == aktivtBehandlingssteg || Behandlingssteg.IVERKSETT_VEDTAK == aktivtBehandlingssteg) {
            if (behandlingsstegDto is BehandlingsstegFatteVedtaksstegDto) {
                hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)

                aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
                if (aktivtBehandlingssteg == Behandlingssteg.IVERKSETT_VEDTAK) {
                    hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
                }
            }
            return
        }
        behandlingskontrollService.behandleStegPåNytt(behandlingId, behandledeSteg)
        hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)

        // sjekk om aktivtBehandlingssteg kan autoutføres
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        if (aktivtBehandlingssteg in
            listOf(
                Behandlingssteg.FORELDELSE,
                Behandlingssteg.VILKÅRSVURDERING,
            )
        ) {
            hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
        }
    }

    @Deprecated("Skal bruke håndterStegAutomatisk. Kan fjernes når den er testet OK")
    @Transactional
    fun håndterStegAutomatiskGAMMEL(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet")
        }
        if (behandling.regelverk == Regelverk.EØS) {
            throw Feil("Behandling med id=$behandlingId behandles etter EØS-regelverket, og skal dermed ikke behandles automatisk.")
        }
        var aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        val behandledeSteg = aktivtBehandlingssteg.name
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg")
        }
        if (behandling.saksbehandlingstype == Saksbehandlingstype.ORDINÆR) {
            throw Feil(
                message =
                    "Behandling med id=$behandlingId er satt til ordinær saksbehandling. " +
                        "Kan ikke saksbehandle den automatisk",
            )
        }
        while (aktivtBehandlingssteg != Behandlingssteg.AVSLUTTET) {
            hentStegInstans(aktivtBehandlingssteg).utførStegAutomatisk(behandlingId)
            if (aktivtBehandlingssteg == Behandlingssteg.IVERKSETT_VEDTAK) {
                break
            }
            aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        }
    }

    @Transactional
    fun håndterStegAutomatisk(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)

        håndterStegAutomatisk(behandling, aktivtBehandlingssteg)
    }

    @Transactional
    fun håndterStegAutomatisk(
        behandling: Behandling,
        aktivtBehandlingssteg: Behandlingssteg,
    ) {
        validerAtBehandlingIkkeErAvsluttet(behandling)
        validerAtUtomatiskBehandlingIkkeErEøs(behandling)
        validerAtBehandlingIkkeErPåVent(behandlingId = behandling.id, erBehandlingPåVent = behandlingskontrollService.erBehandlingPåVent(behandling.id), behandledeSteg = aktivtBehandlingssteg.name)
        validerAtBehandlingErAutomatisk(behandling)

        if (aktivtBehandlingssteg != Behandlingssteg.AVSLUTTET) {
            hentStegInstans(aktivtBehandlingssteg).utførStegAutomatisk(behandling.id)

            if (aktivtBehandlingssteg != Behandlingssteg.IVERKSETT_VEDTAK) {
                val nesteSteg = hentAktivBehandlingssteg(behandling.id)
                håndterStegAutomatisk(behandling, nesteSteg)
            }
        }
    }

    @Transactional
    fun gjenopptaSteg(behandlingId: UUID) {
        var aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)

        hentStegInstans(aktivtBehandlingssteg).gjenopptaSteg(behandlingId)

        // Autoutfør brevmottaker steg og verge steg om verge informasjon er kopiert fra fagsystem
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        when (aktivtBehandlingssteg) {
            Behandlingssteg.BREVMOTTAKER, Behandlingssteg.VERGE -> hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
            else -> return
        }
    }

    @Transactional
    fun angreSendTilBeslutter(behandling: Behandling) {
        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandling.id)

        if (behandlingsstegstilstand?.behandlingssteg != Behandlingssteg.FATTE_VEDTAK) {
            throw Feil("Kan ikke angre send til beslutter når behandlingen er i steg ${behandlingsstegstilstand?.behandlingssteg}")
        }

        if (behandling.status != Behandlingsstatus.FATTER_VEDTAK) {
            throw Feil("Kan ikke angre send til beslutter når behandlingen har status ${behandling.status}")
        }

        behandlingskontrollService.behandleStegPåNytt(behandling.id, Behandlingssteg.FORESLÅ_VEDTAK)
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

    private fun hentAktivBehandlingssteg(behandlingId: UUID): Behandlingssteg {
        val aktivtBehandlingssteg =
            behandlingskontrollService.finnAktivtSteg(behandlingId)
                ?: throw Feil(
                    message = "Behandling $behandlingId har ikke noe aktiv steg",
                    frontendFeilmelding = "Behandling $behandlingId har ikke noe aktiv steg",
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
            throw Feil(message = "Steg $aktivtBehandlingssteg er ikke implementer ennå")
        }

        return aktivtBehandlingssteg
    }

    private fun hentStegInstans(behandlingssteg: Behandlingssteg): IBehandlingssteg {
        return steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
            ?: error("Finner ikke behandlingssteg $behandlingssteg")
    }
}
