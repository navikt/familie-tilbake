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
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
            val gammelBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                    behandlingId, nesteStegMetaData.behandlingssteg)
            when {
                gammelBehandlingsstegstilstand == null -> {
                    settBehandlingsstegOgStatus(behandlingId, nesteStegMetaData)
                }
                else -> {
                    oppdaterBehandlingsstegsstaus(behandlingId, nesteStegMetaData)
                }
            }
        } else {
            log.info("Behandling har allerede et aktivt steg=${aktivtStegstilstand.behandlingssteg} " +
                     "med status=${aktivtStegstilstand.behandlingsstegsstatus}")
        }
    }

    @Transactional
    fun tilbakehoppBehandlingssteg(behandlingId: UUID, behandlingsstegMedStatus: BehandlingsstegMedStatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så kan ikke forsette til ${behandlingsstegMedStatus.behandlingssteg}")
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val behandletSteg = behandlingsstegstilstand.filter { it.behandlingssteg.kanSaksbehandles }
        behandletSteg.map { behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = AVBRUTT)) }
        oppdaterBehandlingsstegsstaus(behandlingId, behandlingsstegMedStatus)
    }

    @Transactional
    fun oppdaterBehandlingsstegsstaus(behandlingId: UUID, behandlingsstegMedStatus: BehandlingsstegMedStatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så status=${behandlingsstegMedStatus.behandlingsstegstatus} kan ikke oppdateres")
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                behandlingId, behandlingsstegMedStatus.behandlingssteg)
                                       ?: throw Feil(message = "Behandling med id=$behandlingId og " +
                                                               "steg=${behandlingsstegMedStatus.behandlingssteg} finnes ikke")
        behandlingsstegstilstandRepository.update(behandlingsstegstilstand.copy(
                behandlingsstegsstatus = behandlingsstegMedStatus.behandlingsstegstatus))
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

    private fun finnNesteBehandlingsstegMedStatus(behandling: Behandling,
                                                  stegstilstand: List<Behandlingsstegstilstand>): BehandlingsstegMedStatus {
        if (stegstilstand.isEmpty()) {
            return when {
                kanSendeVarselsbrev(behandling) -> BehandlingsstegMedStatus(Behandlingssteg.VARSEL, VENTER)
                !harMottattGrunnlag(behandling) -> BehandlingsstegMedStatus(Behandlingssteg.GRUNNLAG, VENTER)
                else -> BehandlingsstegMedStatus(Behandlingssteg.FAKTA, KLAR)
            }
        }
        val sisteUtførteSteg = stegstilstand.filter { Behandlingsstegstatus.erStegUtført(it.behandlingsstegsstatus) }
                .maxByOrNull { it.sporbar.endret.endretTid }!!.behandlingssteg
        return BehandlingsstegMedStatus(Behandlingssteg.finnNesteBehandlingssteg(sisteUtførteSteg), KLAR)
    }

    private fun settBehandlingsstegOgStatus(behandlingId: UUID,
                                            nesteStegMedStatus: BehandlingsstegMedStatus) {
        // startet nytt behandlingssteg
        val nybehandlingstegstilstand = behandlingsstegstilstandRepository.insert(
                Behandlingsstegstilstand(behandlingId = behandlingId,
                                         behandlingssteg = nesteStegMedStatus.behandlingssteg,
                                         behandlingsstegsstatus = STARTET))
        // oppdaterte behandlingsteg med riktig status
        behandlingsstegstilstandRepository.update(nybehandlingstegstilstand.copy(
                behandlingsstegsstatus = nesteStegMedStatus.behandlingsstegstatus))
    }

    private fun kanSendeVarselsbrev(behandling: Behandling): Boolean {
        return Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL == behandling.aktivtFagsystem.tilbakekrevingsvalg
               && !behandling.manueltOpprettet
    }

    private fun harMottattGrunnlag(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id)
    }

}
