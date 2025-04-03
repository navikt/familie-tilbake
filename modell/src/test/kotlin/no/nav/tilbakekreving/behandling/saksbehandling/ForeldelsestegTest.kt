package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.til
import org.junit.jupiter.api.Test

class ForeldelsestegTest {
    @Test
    fun `vurdering av deler av periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 31.januar),
                                kravgrunnlagPeriode(1.februar til 28.februar),
                            ),
                    ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar til 31.januar,
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstending() shouldBe false
    }

    @Test
    fun `vurdering av deler hele perioden`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 31.januar),
                                kravgrunnlagPeriode(1.februar til 28.februar),
                            ),
                    ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar til 31.januar,
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )
        foreldelsesteg.vurderForeldelse(
            1.februar til 28.februar,
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstending() shouldBe true
    }
}
