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
    fun bestemBehandlingsstegogstatus(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            return
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivStegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)

        if (aktivStegstilstand == null) {
            val nesteStegMetaData = finnNesteBehandlingsstegMedStatus(behandling, behandlingsstegstilstand)
            when {
                behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                        behandlingId, nesteStegMetaData.behandlingssteg) == null -> {
                    settBehandlingsstegOgStatus(behandlingId, nesteStegMetaData)
                }
                else -> {
                    oppdaterBehandlingsstegsstaus(behandlingId, nesteStegMetaData)
                }
            }
        } else {
            log.info("Behandling har allerede et aktiv steg=${aktivStegstilstand.behandlingssteg} " +
                     "med status=${aktivStegstilstand.behandlingsstegsstatus}")
        }
    }

    @Transactional
    fun tilbakehoppBehandlingssteg(behandlingId: UUID, behandlingsstegMetaData: BehandlingsstegMetaData) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så kan ikke forsette til ${behandlingsstegMetaData.behandlingssteg}")
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val behandletSteg = behandlingsstegstilstand.filter { it.behandlingssteg.kanSaksbehandles }
        behandletSteg.map { behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = AVBRUTT)) }
        oppdaterBehandlingsstegsstaus(behandlingId, behandlingsstegMetaData)
    }

    @Transactional
    fun oppdaterBehandlingsstegsstaus(behandlingId: UUID, behandlingsstegMetaData: BehandlingsstegMetaData) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet()) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                       "så status=${behandlingsstegMetaData.behandlingsstegstatus} kan ikke oppdateres")
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                behandlingId, behandlingsstegMetaData.behandlingssteg)
                                       ?: throw Feil(message = "Behandling med id=$behandlingId og " +
                                                               "steg=${behandlingsstegMetaData.behandlingssteg} finnes ikke")
        behandlingsstegstilstandRepository.update(behandlingsstegstilstand.copy(
                behandlingsstegsstatus = behandlingsstegMetaData.behandlingsstegstatus))
    }

    fun finnAktivtSteg(behandlingId: UUID): Behandlingssteg? {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return finnAktivStegstilstand(behandlingsstegstilstand)?.behandlingssteg
    }

    fun finnAktivStegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>): Behandlingsstegstilstand? {
        return behandlingsstegstilstand
                .filter { Behandlingsstegstatus.erStegAktiv(it.behandlingsstegsstatus) }
                .getOrNull(0) //forutsetter at behandling kan ha kun et aktiv steg om gangen
    }

    private fun finnNesteBehandlingsstegMedStatus(behandling: Behandling,
                                                  stegstilstand: List<Behandlingsstegstilstand>): BehandlingsstegMetaData {
        if (stegstilstand.isEmpty()) {
            return when {
                kanSendeVarselsbrev(behandling) -> BehandlingsstegMetaData(Behandlingssteg.VARSEL, VENTER)
                !harMottattGrunnlag(behandling) -> BehandlingsstegMetaData(Behandlingssteg.GRUNNLAG, VENTER)
                else -> BehandlingsstegMetaData(Behandlingssteg.FAKTA, KLAR)
            }
        }
        val sisteUtførteSteg = stegstilstand.filter { Behandlingsstegstatus.erStegUtført(it.behandlingsstegsstatus) }
                .maxByOrNull { it.sporbar.endret.endretTid }!!.behandlingssteg
        return BehandlingsstegMetaData(Behandlingssteg.finnNesteBehandlingssteg(sisteUtførteSteg), KLAR)
    }

    private fun settBehandlingsstegOgStatus(behandlingId: UUID,
                                            nesteStegMetaData: BehandlingsstegMetaData) {
        // startet nytt behandlingssteg
        val nybehandlingstegstilstand = behandlingsstegstilstandRepository.insert(
                Behandlingsstegstilstand(behandlingId = behandlingId,
                                         behandlingssteg = nesteStegMetaData.behandlingssteg,
                                         behandlingsstegsstatus = STARTET))
        // oppdaterte behandlingsteg med riktig status
        behandlingsstegstilstandRepository.update(nybehandlingstegstilstand.copy(
                behandlingsstegsstatus = nesteStegMetaData.behandlingsstegstatus))
    }

    private fun kanSendeVarselsbrev(behandling: Behandling): Boolean {
        return Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL == behandling.aktivtFagsystem.tilbakekrevingsvalg
               && !behandling.manueltOpprettet
    }

    private fun harMottattGrunnlag(behandling: Behandling): Boolean {
        return kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id)
    }

}
