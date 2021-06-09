package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingsvedtakService(private val behandlingRepository: BehandlingRepository,
                               private val tilbakeBeregningService: TilbakekrevingsberegningService) {

    @Transactional
    fun opprettBehandlingsvedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        val beregningsresultat = tilbakeBeregningService.beregn(behandlingId)
        val behandlingsresultatstype = utledBehandlingsresultatype(beregningsresultat.vedtaksresultat)

        val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                                  iverksettingsstatus = Iverksettingsstatus.IKKE_IVERKSATT)
        val behandlingsresultat = Behandlingsresultat(type = behandlingsresultatstype,
                                                      behandlingsvedtak = behandlingsvedtak)
        behandlingRepository.update(behandling.copy(resultater = setOf(behandlingsresultat)))
    }

    @Transactional
    fun oppdaterBehandlingsvedtak(behandlingId: UUID, iverksettingsstatus: Iverksettingsstatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivBehandlingsresultat = requireNotNull(behandling.sisteResultat) { "Behandlingsresultat kan ikke være null" }
        val behandlingsvedtak =
                requireNotNull(aktivBehandlingsresultat.behandlingsvedtak) { "Behandlingsvedtak kan ikke være null" }
        val oppdatertBehandlingsresultat = aktivBehandlingsresultat
                .copy(behandlingsvedtak = behandlingsvedtak.copy(iverksettingsstatus = iverksettingsstatus))
        behandlingRepository.update(behandling.copy(resultater = setOf(oppdatertBehandlingsresultat)))
    }

    private fun utledBehandlingsresultatype(vedtaksresultat: Vedtaksresultat): Behandlingsresultatstype {
        return when (vedtaksresultat) {
            Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
            Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
            Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
        }
    }
}
