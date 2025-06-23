package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PeriodeService(
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val foreldelseService: ForeldelseService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val fagsakRepository: FagsakRepository,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
) {
    fun erEnsligForsørgerOgPerioderLike(behandlingId: UUID): SkalSammenslåPerioder {
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandlingId.toString())
        return when {
            harKunEnPeriode(behandlingId) -> SkalSammenslåPerioder.IKKE_AKTUELT
            erPerioderLike(behandlingId, logContext) && fagsak.ytelsestype.tilTema() == Tema.ENF -> SkalSammenslåPerioder.JA
            else -> SkalSammenslåPerioder.IKKE_AKTUELT
        }
    }

    private fun harKunEnPeriode(behandlingId: UUID): Boolean {
        val harEnFaktaPeriode = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId).feilutbetaltePerioder.size == 1
        val harEnVilkårsperiode = vilkårsvurderingService.hentVilkårsvurdering(behandlingId).perioder.size == 1

        return harEnFaktaPeriode || harEnVilkårsperiode
    }

    private fun erPerioderLike(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) =
        faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
            foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId, logContext) &&
            vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId, logContext)

    fun erPerioderSammenslått(behandlingId: UUID): Boolean {
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        return vedtaksbrevsoppsummering?.skalSammenslåPerioder == SkalSammenslåPerioder.JA
    }
}
