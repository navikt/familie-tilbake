package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.til
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

    @Test
    fun `ulik foreldelse på splittet periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 28.februar),
                            ),
                    ),
                ),
            )

        foreldelsesteg.splittPerioder(
            listOf(
                1.januar til 31.januar,
                1.februar til 28.februar,
            ),
        )
        foreldelsesteg.vurderForeldelse(
            1.januar til 31.januar,
            Foreldelsesteg.Vurdering.Foreldet("Deler av perioden er foreldet fordi grunner", LocalDate.now().minusDays(7)),
        )
        foreldelsesteg.erFullstending() shouldBe false

        foreldelsesteg.vurderForeldelse(
            1.februar til 28.februar,
            Foreldelsesteg.Vurdering.IkkeForeldet("Hele greia er ikke foreldet"),
        )

        foreldelsesteg.erFullstending() shouldBe true
        foreldelsesteg.erPeriodeForeldet(1.januar til 31.januar) shouldBe true
        foreldelsesteg.erPeriodeForeldet(1.februar til 28.februar) shouldBe false
    }

    @Test
    fun `vurdering av foreldelse på full periode etter splitt`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 28.februar),
                            ),
                    ),
                ),
            )

        foreldelsesteg.splittPerioder(
            listOf(
                1.januar til 31.januar,
                1.februar til 28.februar,
            ),
        )

        shouldThrow<NoSuchElementException> {
            foreldelsesteg.vurderForeldelse(1.januar til 28.februar, Foreldelsesteg.Vurdering.IkkeForeldet(""))
        }
    }
}
