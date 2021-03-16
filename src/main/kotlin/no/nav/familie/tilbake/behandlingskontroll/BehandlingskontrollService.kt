package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AVBRUTT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.KLAR
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.STARTET
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.VENTER
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingskontrollService(private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
                                 private val behandlingRepository: BehandlingRepository,
                                 private val kravgrunnlagRepository: KravgrunnlagRepository) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun fortsettBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            return
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtStegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)

        if (aktivtStegstilstand == null) {
            val nesteStegMetaData = finnNesteBehandlingsstegMedStatus(behandling, behandlingsstegstilstand)
            persisterBehandlingsstegOgStatus(behandlingId, nesteStegMetaData)
        } else {
            log.info("Behandling har allerede et aktivt steg=${aktivtStegstilstand.behandlingssteg} " +
                     "med status=${aktivtStegstilstand.behandlingsstegsstatus}")
        }
    }

    @Transactional
    fun tilbakehoppBehandlingssteg(behandlingId: UUID, behandlingsstegsinfo: Behandlingsstegsinfo) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så kan ikke forsette til ${behandlingsstegsinfo.behandlingssteg}")
        }
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
                behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtBehandlingssteg = finnAktivStegstilstand(behandlingsstegstilstand)
                                    ?: throw Feil("Behandling med id=$behandlingId har ikke noe aktivt steg")
        // steg som kan behandles, kan avbrytes
        if (aktivtBehandlingssteg.behandlingssteg.kanSaksbehandles) {
            behandlingsstegstilstandRepository.update(aktivtBehandlingssteg.copy(behandlingsstegsstatus = AVBRUTT))
            persisterBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo)
        }
    }

    @Transactional
    fun settBehandlingPåVent(behandlingId: UUID, venteårsak: Venteårsak, tidsfrist: LocalDate) {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
                behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)
                                             ?: throw Feil(message = "Behandling $behandlingId " +
                                                                     "har ikke aktivt steg",
                                                           frontendFeilmelding = "Behandling $behandlingId " +
                                                                                 "har ikke aktivt steg")
        behandlingsstegstilstandRepository.update(aktivtBehandlingsstegstilstand.copy(behandlingsstegsstatus = VENTER,
                                                                                      venteårsak = venteårsak,
                                                                                      tidsfrist = tidsfrist))
    }

    fun finnAktivtSteg(behandlingId: UUID): Behandlingssteg? {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return finnAktivStegstilstand(behandlingsstegstilstand)?.behandlingssteg
    }

    fun finnAktivStegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>): Behandlingsstegstilstand? {
        return behandlingsstegstilstand
                .firstOrNull { Behandlingsstegstatus.erStegAktiv(it.behandlingsstegsstatus) }
        //forutsetter at behandling kan ha kun et aktiv steg om gangen
    }

    @Transactional
    fun oppdaterBehandlingsstegsstaus(behandlingId: UUID, behandlingsstegsinfo: Behandlingsstegsinfo) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så status=${behandlingsstegsinfo.behandlingsstegstatus} kan ikke oppdateres")
        }
        val behandlingsstegstilstand =
                behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(behandlingId,
                                                                                        behandlingsstegsinfo.behandlingssteg)
                ?: throw Feil(message = "Behandling med id=$behandlingId og " +
                                        "steg=${behandlingsstegsinfo.behandlingssteg} finnes ikke")
        behandlingsstegstilstandRepository
                .update(behandlingsstegstilstand.copy(behandlingsstegsstatus =  behandlingsstegsinfo.behandlingsstegstatus,
                                                      venteårsak = behandlingsstegsinfo.venteårsak,
                                                      tidsfrist = behandlingsstegsinfo.tidsfrist))
    }

    private fun settBehandlingsstegOgStatus(behandlingId: UUID,
                                            nesteStegMedStatus: Behandlingsstegsinfo) {
        // startet nytt behandlingssteg
        val nybehandlingstegstilstand =
                behandlingsstegstilstandRepository
                        .insert(Behandlingsstegstilstand(behandlingId = behandlingId,
                                                         behandlingssteg = nesteStegMedStatus.behandlingssteg,
                                                         venteårsak = nesteStegMedStatus.venteårsak,
                                                         tidsfrist = nesteStegMedStatus.tidsfrist,
                                                         behandlingsstegsstatus = STARTET))
        // oppdaterte behandlingsteg med riktig status
        behandlingsstegstilstandRepository
                .update(nybehandlingstegstilstand.copy(behandlingsstegsstatus = nesteStegMedStatus.behandlingsstegstatus))
    }

    private fun persisterBehandlingsstegOgStatus(behandlingId: UUID,
                                                 behandlingsstegsinfo: Behandlingsstegsinfo) {
        val gammelBehandlingsstegstilstand =
                behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(behandlingId,
                                                                                        behandlingsstegsinfo.behandlingssteg)
        when (gammelBehandlingsstegstilstand) {
            null -> {
                settBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo)
            }
            else -> {
                oppdaterBehandlingsstegsstaus(behandlingId, behandlingsstegsinfo)
            }
        }
    }

    private fun finnNesteBehandlingsstegMedStatus(behandling: Behandling,
                                                  stegstilstand: List<Behandlingsstegstilstand>): Behandlingsstegsinfo {
        if (stegstilstand.isEmpty()) {
            return when {
                //setter tidsfristen fra opprettelse dato
                kanSendeVarselsbrev(behandling) -> lagBehandlingsstegsinfo(Behandlingssteg.VARSEL,
                                                                           VENTER,
                                                                           Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                           behandling.opprettetDato)
                !harAktivtGrunnlag(behandling) -> lagBehandlingsstegsinfo(Behandlingssteg.GRUNNLAG,
                                                                          VENTER,
                                                                          Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                                                          behandling.opprettetDato)
                else -> lagBehandlingsstegsinfo(Behandlingssteg.FAKTA, KLAR)
            }
        }

        val finnesAvbruttSteg = stegstilstand.any { AVBRUTT == it.behandlingsstegsstatus }
        if (finnesAvbruttSteg) {
            //forutsetter behandling har et AVBRUTT steg om gangen
            val avbruttSteg = stegstilstand.first { AVBRUTT == it.behandlingsstegsstatus }
            return lagBehandlingsstegsinfo(avbruttSteg.behandlingssteg, KLAR)
        }

        val sisteUtførteSteg = stegstilstand.filter { Behandlingsstegstatus.erStegUtført(it.behandlingsstegsstatus) }
                .maxByOrNull { it.sporbar.endret.endretTid }!!.behandlingssteg

        if (Behandlingssteg.VARSEL == sisteUtførteSteg) {
            return håndterOmSisteUtførteStegErVarsel(behandling)
        }
        return lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.finnNesteBehandlingssteg(sisteUtførteSteg), KLAR)
    }

    private fun håndterOmSisteUtførteStegErVarsel(behandling: Behandling): Behandlingsstegsinfo {
        return when {
            erKravgrunnlagSperret(behandling) -> {
                val kravgrunnlag = kravgrunnlagRepository
                        .findByBehandlingIdAndAktivIsTrueAndSperretTrue(behandling.id)

                // setter tidsfristen fra sperret dato
                lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.GRUNNLAG,
                                     behandlingsstegstatus = VENTER,
                                     venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                     tidsfrist = kravgrunnlag.sporbar.endret.endretTid
                                             .toLocalDate())
            }
            harAktivtGrunnlag(behandling) -> {
                lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.FAKTA,
                                        behandlingsstegstatus = KLAR)
            }
            // setter tidsfristen fra opprettelse dato
            else -> lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.GRUNNLAG,
                                            behandlingsstegstatus = VENTER,
                                            venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                            tidsfrist = behandling.opprettetDato)
        }
    }

    private fun kanSendeVarselsbrev(behandling: Behandling): Boolean {
        return Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL == behandling.aktivtFagsystem.tilbakekrevingsvalg
               && !behandling.manueltOpprettet
    }

    private fun harAktivtGrunnlag(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id)
    }

    private fun erKravgrunnlagSperret(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandling.id)
    }

    private fun lagBehandlingsstegsinfo(behandlingssteg: Behandlingssteg,
                                        behandlingsstegstatus: Behandlingsstegstatus,
                                        venteårsak: Venteårsak? = null,
                                        tidsfrist: LocalDate? = null): Behandlingsstegsinfo {

        return Behandlingsstegsinfo(behandlingssteg = behandlingssteg,
                                    behandlingsstegstatus = behandlingsstegstatus,
                                    venteårsak = venteårsak,
                                    tidsfrist = venteårsak?.defaultVenteTidIUker?.let { tidsfrist?.plusWeeks(it) })
    }

}
