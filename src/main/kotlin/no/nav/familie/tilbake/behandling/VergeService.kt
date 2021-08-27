package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VergeService(private val behandlingRepository: BehandlingRepository,
                   private val historikkTaskService: HistorikkTaskService,
                   private val behandlingskontrollService: BehandlingskontrollService) {

    @Transactional
    fun lagreVerge(behandlingId: UUID, vergeDto: VergeDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerBehandling(behandling)

        val verge = tilDomene(vergeDto)
        val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet() + verge)
        behandlingRepository.update(oppdatertBehandling)
        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.VERGE_OPPRETTET,
                                              Aktør.SAKSBEHANDLER)
    }

    @Transactional
    fun opprettVergeSteg(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerBehandling(behandling)
        behandlingskontrollService.behandleVergeSteg(behandlingId)
    }

    @Transactional
    fun fjernVerge(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val finnesAktivVerge = behandling.verger.any { it.aktiv }

        if (finnesAktivVerge) {
            val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet())
            behandlingRepository.update(oppdatertBehandling)
            historikkTaskService.lagHistorikkTask(behandling.id,
                                                  TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                                                  Aktør.SAKSBEHANDLER)
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(Behandlingssteg.VERGE,
                                                                                          Behandlingsstegstatus.TILBAKEFØRT))
        }
    }

    @Transactional(readOnly = true)
    fun hentVerge(behandlingId: UUID): VergeDto? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        return behandling.aktivVerge?.let { tilRespons(it) }
    }

    private fun validerBehandling(behandling: Behandling) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        if (behandlingskontrollService.erBehandlingPåVent(behandling.id)) {
            throw Feil("Behandling med id=${behandling.id} er på vent.",
                       frontendFeilmelding = "Behandling med id=${behandling.id} er på vent.",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun tilDomene(vergeDto: VergeDto): Verge {
        return Verge(ident = vergeDto.ident,
                     orgNr = vergeDto.orgNr,
                     aktiv = true,
                     type = vergeDto.type,
                     navn = vergeDto.navn,
                     kilde = vergeDto.kilde,
                     begrunnelse = vergeDto.begrunnelse)
    }

    private fun tilRespons(verge: Verge): VergeDto {
        return VergeDto(ident = verge.ident,
                        orgNr = verge.orgNr,
                        type = verge.type,
                        navn = verge.navn,
                        kilde = verge.kilde,
                        begrunnelse = verge.begrunnelse)
    }
}
