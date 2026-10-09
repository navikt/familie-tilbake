package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UendretPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.Forskjell
import no.nav.tilbakekreving.test.april
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class EndretPeriodeTest {
    @Test
    fun `sammendrag av endret periode uten etterfølgende uendret periode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 10.januar(2021) til 31.januar(2021)
        val endret = Forskjell.EndretPeriode(
            periode = periode,
            nyPeriode = nyPeriode,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )

        endret.tilSammendrag() shouldBe listOf(
            EndretPeriodeDto(
                fom = nyPeriode.fom,
                tom = nyPeriode.tom,
                gammelPeriode = PeriodeDto(periode.fom, periode.tom),
                gammeltBeløp = 100,
                nyttBeløp = 150,
            ),
        )
    }

    @Test
    fun `sammendrag av endret periode med flere etterfølgende uendrede perioder`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val endret = Forskjell.EndretPeriode(
            periode = periode,
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )

        val resultat = endret.slåSammen(
            Forskjell.UendretPeriode(
                periode = 1.februar(2021) til 28.februar(2021),
                gammeltBeløp = 200.kroner,
                nyttBeløp = 200.kroner,
            ),
        ).shouldBeInstanceOf<Forskjell.EndretPeriode>().slåSammen(
            Forskjell.UendretPeriode(
                periode = 1.mars(2021) til 31.mars(2021),
                gammeltBeløp = 300.kroner,
                nyttBeløp = 300.kroner,
            ),
        ).shouldBeInstanceOf<Forskjell.EndretPeriode>()

        resultat.tilSammendrag() shouldBe listOf(
            EndretPeriodeDto(
                fom = periode.fom,
                tom = periode.tom,
                gammelPeriode = PeriodeDto(periode.fom, periode.tom),
                gammeltBeløp = 100,
                nyttBeløp = 150,
            ),
            UendretPeriodeDto(1.februar(2021), 31.mars(2021), 500),
        )
    }

    @Test
    fun `sammendrag av to endrede perioder med flere uendrede perioder mellom`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )

        val resultat = endret.slåSammen(
            Forskjell.UendretPeriode(
                periode = 1.februar(2021) til 28.februar(2021),
                gammeltBeløp = 200.kroner,
                nyttBeløp = 200.kroner,
            ),
        ).shouldBeInstanceOf<Forskjell.EndretPeriode>().slåSammen(
            Forskjell.UendretPeriode(
                periode = 1.mars(2021) til 31.mars(2021),
                gammeltBeløp = 300.kroner,
                nyttBeløp = 300.kroner,
            ),
        ).shouldBeInstanceOf<Forskjell.EndretPeriode>().slåSammen(
            Forskjell.EndretPeriode(
                periode = 1.april(2021) til 30.april(2021),
                nyPeriode = null,
                gammeltBeløp = 400.kroner,
                nyttBeløp = 450.kroner,
                etterfølgende = null,
            ),
        ).shouldBeInstanceOf<Forskjell.EndretPeriode>()

        resultat.tilSammendrag() shouldBe listOf(
            EndretPeriodeDto(
                fom = 1.januar(2021),
                tom = 30.april(2021),
                gammelPeriode = PeriodeDto(1.januar(2021), 30.april(2021)),
                gammeltBeløp = 1000,
                nyttBeløp = 1100,
            ),
        )
    }

    @Test
    fun `slår ikke sammen endret og uendret periode`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )
        val uendret = Forskjell.UendretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 200.kroner,
        )

        val resultat = endret.slåSammen(uendret).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 31.januar(2021))
        resultat.nyPeriode shouldBe null
        resultat.gammeltBeløp shouldBe 100.kroner
        resultat.nyttBeløp shouldBe 150.kroner
    }

    @Test
    fun `slår sammen med uendret periode i mellom`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )

        val resultat = endret
            .slåSammen(
                Forskjell.UendretPeriode(
                    periode = 1.februar(2021) til 28.februar(2021),
                    gammeltBeløp = 200.kroner,
                    nyttBeløp = 200.kroner,
                ),
            )
            ?.slåSammen(
                Forskjell.EndretPeriode(
                    periode = 1.mars(2021) til 31.mars(2021),
                    nyPeriode = null,
                    gammeltBeløp = 100.kroner,
                    nyttBeløp = 150.kroner,
                    etterfølgende = null,
                ),
            ).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 31.mars(2021))
        resultat.nyPeriode shouldBe null
        resultat.gammeltBeløp shouldBe 400.kroner
        resultat.nyttBeløp shouldBe 500.kroner
    }

    @Test
    fun `slår sammen perioder hvor siste periode har ny tom dato`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = 1.februar(2021) til 14.februar(2021),
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
            etterfølgende = null,
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
            etterfølgende = null,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = null,
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
            etterfølgende = null,
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
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )
        val other = Forskjell.EndretPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyPeriode = null,
            gammeltBeløp = 200.kroner,
            nyttBeløp = 250.kroner,
            etterfølgende = null,
        )

        val resultat = endret.slåSammen(other).shouldBeInstanceOf<Forskjell.EndretPeriode>()
        resultat.periode shouldBe (1.januar(2021) til 28.februar(2021))
        resultat.nyPeriode.shouldBeNull()
        resultat.gammeltBeløp shouldBe 300.kroner
        resultat.nyttBeløp shouldBe 400.kroner
    }

    @Test
    fun `slår ikke sammen ulike typer forskjell`() {
        val endret = Forskjell.EndretPeriode(
            periode = 1.januar(2021) til 31.januar(2021),
            nyPeriode = null,
            gammeltBeløp = 100.kroner,
            nyttBeløp = 150.kroner,
            etterfølgende = null,
        )
        val other = Forskjell.NyPeriode(
            periode = 1.februar(2021) til 28.februar(2021),
            nyttBeløp = 200.kroner,
        )

        endret.slåSammen(other).shouldBeNull()
    }
}
