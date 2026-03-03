package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test

class ForeldelsestegTest {
    @Test
    fun `vurdering av deler av periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                            kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstendig() shouldBe false
    }

    @Test
    fun `vurdering av deler hele perioden`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                            kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )
        foreldelsesteg.vurderForeldelse(
            1.februar(2021) til 28.februar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstendig() shouldBe true
    }

    @Test
    fun `sjekk foreldelse på deler av splittet periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(1.januar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))

        foreldelsesteg.erPeriodeForeldet(1.januar(2021) til 31.januar(2021)) shouldBe false
    }
}
