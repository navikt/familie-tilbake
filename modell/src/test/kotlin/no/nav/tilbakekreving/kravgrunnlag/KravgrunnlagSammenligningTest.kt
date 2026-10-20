package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.frontend.models.UendretPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test

class KravgrunnlagSammenligningTest {
    @Test
    fun `sammendrag - ingen endringer`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        sammendrag(
            nåværende = listOf(periode to 2000.kroner),
            oppdatert = listOf(periode to 2000.kroner),
        ) shouldBe listOf(
            UendretPeriodeDto(periode.fom, periode.tom, 2000),
        )
    }

    @Test
    fun `endring av datoer i eksisterende periode`() {
        sammenlign(
            nåværende = listOf((1.januar(2021) til 31.januar(2021)) to 2000.kroner),
            oppdatert = listOf((1.januar(2021) til 20.januar(2021)) to 2000.kroner),
        ).resultat().shouldBeSingle().shouldBeInstanceOf<KravgrunnlagSammenligning.Forskjell.EndretPeriode> {
            it.periode shouldBe (1.januar(2021) til 31.januar(2021))
            it.nyPeriode shouldBe (1.januar(2021) til 20.januar(2021))
        }
    }

    @Test
    fun `nytt kravgrunnlag slår sammen perioder`() {
        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            sammenlign(
                nåværende = listOf(
                    (1.januar(2021) til 10.januar(2021)) to 2000.kroner,
                    (20.januar(2021) til 31.januar(2021)) to 2000.kroner,
                ),
                oppdatert = listOf((1.januar(2021) til 31.januar(2021)) to 2000.kroner),
            )
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagMedSammenslåttPerioder
    }
}
