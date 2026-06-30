package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.feilutbetalteBeløp
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test

class KravgrunnlagSammenligningTest {
    @Test
    fun `ulike perioder fører til utenfor scope exception`() {
        shouldThrow<ModellFeil.UtenforScopeException> {
            KravgrunnlagSammenligning(
                originaltKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)))),
                nyttKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = 1.januar(2021) til 20.januar(2021)))),
                sporing = Sporing("", ""),
            )
        }
    }

    @Test
    fun `flere perioder fører til utenfor scope exception`() {
        shouldThrow<ModellFeil.UtenforScopeException> {
            KravgrunnlagSammenligning(
                originaltKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)))),
                nyttKravgrunnlag = kravgrunnlag(
                    perioder = listOf(
                        kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                        kravgrunnlagPeriode(periode = 1.februar(2021) til 28.februar(2021)),
                    ),
                ),
                sporing = Sporing("", ""),
            )
        }
    }

    @Test
    fun `færre perioder fører til utenfor scope exception`() {
        shouldThrow<ModellFeil.UtenforScopeException> {
            KravgrunnlagSammenligning(
                originaltKravgrunnlag = kravgrunnlag(
                    perioder = listOf(
                        kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                        kravgrunnlagPeriode(periode = 1.februar(2021) til 28.februar(2021)),
                    ),
                ),
                nyttKravgrunnlag = kravgrunnlag(
                    perioder = listOf(
                        kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                    ),
                ),
                sporing = Sporing("", ""),
            )
        }
    }

    @Test
    fun `høyere beløp`() {
        val beløp1 = ytelsesbeløp(tilbakekrevesBeløp = 1000.kroner)
        val beløp2 = ytelsesbeløp(tilbakekrevesBeløp = 2000.kroner)
        val forskjell = KravgrunnlagSammenligning(
            originaltKravgrunnlag = kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021), ytelsesbeløp = beløp1 + feilutbetalteBeløp(beløp1)),
                ),
            ),
            nyttKravgrunnlag = kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021), ytelsesbeløp = beløp2 + feilutbetalteBeløp(beløp2)),
                ),
            ),
            sporing = Sporing("", ""),
        ).resultat().shouldBeSingle().shouldBeInstanceOf<KravgrunnlagSammenligning.Forskjell.JustertBeløp>()

        forskjell.differanse shouldBe 1000.kroner
    }
}
