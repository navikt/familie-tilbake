package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
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
        if (harMerEnnEnPeriode(behandlingId) &&
            erPerioderLike(behandlingId) &&
            fagsak.ytelsestype.tilTema() == Tema.ENF
        ) {
            return SkalSammenslåPerioder.JA
        }
        return SkalSammenslåPerioder.IKKE_AKTUELT
    }

    private fun harMerEnnEnPeriode(behandlingId: UUID): Boolean {
        val foreldelsesperioder = foreldelseService.hentAktivVurdertForeldelse(behandlingId)?.foreldelsesperioder
        return faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId).feilutbetaltePerioder.size > 1 ||
                (foreldelsesperioder == null || foreldelsesperioder.size > 1) ||
                vilkårsvurderingService.hentVilkårsvurdering(behandlingId).perioder.size > 1
    }

    private fun erPerioderLike(behandlingId: UUID) =
        faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
            foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId) &&
            vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId)

    fun erPerioderSammenslått(behandlingId: UUID): Boolean {
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        return vedtaksbrevsoppsummering?.skalSammenslåPerioder == SkalSammenslåPerioder.JA
    }
}
