package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class VilkårsvurderingstegEntityTest {
    @Test
    fun `vilkårsvurdering med feil rekkefølge på perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val behandlingId = UUID.randomUUID()
        val vilkårsvurderingFør = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 12.januar(2021)),
                    kravgrunnlagPeriode(15.januar(2021) til 28.januar(2021)),
                ),
            ),
        )

        vilkårsvurderingFør.splittVilkårsvurdering(15.januar(2021))

        val entity = vilkårsvurderingFør.tilEntity(behandlingId).let {
            // Simuler at periodene endrer rekkefølge i DB
            it.copy(vurderinger = it.vurderinger.reversed())
        }

        vilkårsvurderingFør.vurdertePerioderForBrev(emptySet()) shouldBe entity.fraEntity().vurdertePerioderForBrev(emptySet())
    }
}
