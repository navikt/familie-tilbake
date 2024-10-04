package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.tilbake.behandling.FagsakRepository
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
) {
    fun erEnsligForsørgerOgPerioderLike(behandlingId: UUID): Boolean {
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        return faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandlingId) &&
            foreldelseService.sjekkOmForeldelsePerioderErLike(behandlingId) &&
            vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandlingId) &&
            fagsak.ytelsestype.tilTema() == Tema.ENF
    }
}
