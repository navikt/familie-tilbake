package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.Forskjell
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test

class EndretPeriodeTest {
    @Test
    fun `endret periode og uendret periode`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val uendret = Forskjell.UendretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 200.kroner,
        )

        endret.slåSammen(uendret).shouldBeNull()
    }

    @Test
    fun `endring av beløp`() {
        val høyereBeløp = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val lavereBeløp = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 100.kroner,
        )

        høyereBeløp.slåSammen(lavereBeløp).shouldBeNull()
    }

    @Test
    fun `lavere beløp etterfulgt av endret dato`() {
        val lavereBeløp = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 100.kroner,
        )
        val endretPeriode = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 14.februar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 100.kroner,
        )

        val resultat = lavereBeløp.slåSammen(endretPeriode).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode shouldBe (1.januar(2021) til 14.februar(2021))
        resultat.gammeltBeløp shouldBe 300.kroner
        resultat.nyttBeløp shouldBe 200.kroner
    }

    @Test
    fun `endret dato etterfulgt av økning i beløp`() {
        val endretPeriode = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 10.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 100.kroner,
        )
        val øktBeløp = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )

        val resultat = endretPeriode.slåSammen(øktBeløp).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode shouldBe (10.januar(2021) til 28.februar(2021))
        resultat.gammeltBeløp shouldBe 200.kroner
        resultat.nyttBeløp shouldBe 250.kroner
    }

    @Test
    fun `slår sammen perioder hvor siste periode har ny tom dato`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 14.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
        )

        val resultat = endret.slåSammen(other).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode shouldBe (1.januar(2021) til 14.februar(2021))
        resultat.gammeltBeløp shouldBe 300.kroner
        resultat.nyttBeløp shouldBe 400.kroner
    }

    @Test
    fun `slår sammen perioder hvor første periode har fom ny dato`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 10.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
        )

        val resultat = endret.slåSammen(other).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode shouldBe (10.januar(2021) til 28.februar(2021))
        resultat.gammeltBeløp shouldBe 300.kroner
        resultat.nyttBeløp shouldBe 400.kroner
    }

    @Test
    fun `slår sammen perioder uten endring i datoer`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
        )

        val resultat = endret.slåSammen(other).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.gammeltBeløp shouldBe 300.kroner
        resultat.nyttBeløp shouldBe 400.kroner
    }

    @Test
    fun `slår ikke sammen ulike typer forskjell`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = 1.januar(2021) til 31.januar(2021),
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
        )
        val other = Forskjell.NyPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyttBeløp = 200.kroner,
        )

        endret.slåSammen(other).shouldBeNull()
    }
}
