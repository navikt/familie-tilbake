package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class VergeService(private val behandlingRepository: BehandlingRepository,
                   private val historikkTaskService: HistorikkTaskService) {

    fun opprettVerge(behandlingId: UUID, vergeDto: VergeDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val verge = tilDomene(vergeDto)
        val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet() + verge)
        behandlingRepository.update(oppdatertBehandling)
        historikkTaskService.lagHistorikkTask(behandling.id,
                                              TilbakekrevingHistorikkinnslagstype.VERGE_OPPRETTET,
                                              Aktør.SAKSBEHANDLER)

        // TODO Behandlingskontroll
    }

    fun fjernVerge(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val finnesAktivVerge = behandling.verger.any { it.aktiv }

        if (finnesAktivVerge) {
            val oppdatertBehandling = behandling.copy(verger = behandling.verger.map { it.copy(aktiv = false) }.toSet())
            behandlingRepository.update(oppdatertBehandling)
            historikkTaskService.lagHistorikkTask(behandling.id,
                                                  TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                                                  Aktør.SAKSBEHANDLER)
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
}
