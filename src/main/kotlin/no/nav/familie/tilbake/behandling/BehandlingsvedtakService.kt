package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.bigQuery.BigQueryAdapterService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingsvedtakService(
    private val behandlingRepository: BehandlingRepository,
    private val tellerService: TellerService,
    private val tilbakeBeregningService: TilbakekrevingsberegningService,
    private val bigQueryAdapterService: BigQueryAdapterService,
) {
    @Transactional
    fun opprettBehandlingsvedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        val beregning = tilbakeBeregningService.beregn(behandlingId)
        val behandlingsresultatstype = utledBehandlingsresultatstype(beregning.vedtaksresultat())

        val behandlingsvedtak =
            Behandlingsvedtak(
                vedtaksdato = LocalDate.now(),
                iverksettingsstatus = Iverksettingsstatus.IKKE_IVERKSATT,
            )
        val behandlingsresultat =
            Behandlingsresultat(
                type = behandlingsresultatstype,
                behandlingsvedtak = behandlingsvedtak,
            )
        val oppdatertBehandling = behandlingRepository.update(behandling.copy(resultater = setOf(behandlingsresultat)))
        bigQueryAdapterService.oppdaterBigQuery(oppdatertBehandling)

        tellerService.tellVedtak(behandlingsresultatstype, behandling)
    }

    @Transactional
    fun oppdaterBehandlingsvedtak(
        behandlingId: UUID,
        iverksettingsstatus: Iverksettingsstatus,
    ): Behandling {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivBehandlingsresultat = requireNotNull(behandling.sisteResultat) { "Behandlingsresultat kan ikke være null" }
        val behandlingsvedtak =
            requireNotNull(aktivBehandlingsresultat.behandlingsvedtak) { "Behandlingsvedtak kan ikke være null" }
        val oppdatertBehandlingsresultat =
            aktivBehandlingsresultat
                .copy(behandlingsvedtak = behandlingsvedtak.copy(iverksettingsstatus = iverksettingsstatus))
        val oppdatertBehandling = behandlingRepository.update(behandling.copy(resultater = setOf(oppdatertBehandlingsresultat)))
        bigQueryAdapterService.oppdaterBigQuery(oppdatertBehandling)
        return oppdatertBehandling
    }

    private fun utledBehandlingsresultatstype(vedtaksresultat: Vedtaksresultat): Behandlingsresultatstype =
        when (vedtaksresultat) {
            Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
            Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
            Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
        }
}
