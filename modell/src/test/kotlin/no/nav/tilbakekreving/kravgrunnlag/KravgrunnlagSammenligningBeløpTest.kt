package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UendretPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class KravgrunnlagSammenligningBeløpTest {
    @Test
    fun `høyere beløp`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val forskjell = sammenlign(
            nåværende = listOf(periode to 1000.kroner),
            oppdatert = listOf(periode to 2000.kroner),
        ).resultat().shouldBeSingle().shouldBeInstanceOf<KravgrunnlagSammenligning.Forskjell.EndretPeriode>()

        forskjell.gammeltBeløp shouldBe 1000.kroner
        forskjell.nyttBeløp shouldBe 2000.kroner
    }

    @Test
    fun `sammendrag - uendret periode mellom to høyere beløp`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
                periode3 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1000.kroner,
                periode3 to 1500.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode1.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            UendretPeriodeDto(periode2.fom, periode2.tom, 1000),
            EndretPeriodeDto(
                fom = periode3.fom,
                tom = periode3.tom,
                gammelPeriode = PeriodeDto(periode3.fom, periode3.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
        )
    }

    @Test
    fun `sammendrag - endring av beløp for flere perioder slås sammen`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1500.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = 1.januar(2021),
                tom = 28.februar(2021),
                gammelPeriode = PeriodeDto(1.januar(2021), 28.februar(2021)),
                gammeltBeløp = 2000,
                nyttBeløp = 3000,
            ),
        )
    }

    @Test
    fun `sammendrag - endring av beløp med gap mellom periodene slås sammen`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1500.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = 1.januar(2021),
                tom = 31.mars(2021),
                gammelPeriode = PeriodeDto(1.januar(2021), 31.mars(2021)),
                gammeltBeløp = 2000,
                nyttBeløp = 3000,
            ),
        )
    }

    @Test
    fun `sammendrag - negativ endring i beløp`() {
        val periode = 1.januar(2021) til 31.januar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(periode to 2000.kroner),
            oppdatert = listOf(periode to 1000.kroner),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode.fom,
                tom = periode.tom,
                gammelPeriode = PeriodeDto(periode.fom, periode.tom),
                gammeltBeløp = 2000,
                nyttBeløp = 1000,
            ),
        )
    }

    @Test
    fun `sammendrag - økning og reduksjon i beløp`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 2000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 2000.kroner,
                periode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode1.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 2000,
            ),
            EndretPeriodeDto(
                fom = periode2.fom,
                tom = periode2.tom,
                gammelPeriode = PeriodeDto(periode2.fom, periode2.tom),
                gammeltBeløp = 2000,
                nyttBeløp = 1000,
            ),
        )
    }

    @Test
    fun `sammendrag - to reduksjoner i beløp inntil hverandre`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 2000.kroner,
                periode2 to 2000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode2.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode2.tom),
                gammeltBeløp = 4000,
                nyttBeløp = 2000,
            ),
        )
    }

    @Test
    fun `sammendrag - økning, reduksjon og økning i beløp`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 2000.kroner,
                periode3 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1000.kroner,
                periode3 to 1500.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode1.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            EndretPeriodeDto(
                fom = periode2.fom,
                tom = periode2.tom,
                gammelPeriode = PeriodeDto(periode2.fom, periode2.tom),
                gammeltBeløp = 2000,
                nyttBeløp = 1000,
            ),
            EndretPeriodeDto(
                fom = periode3.fom,
                tom = periode3.tom,
                gammelPeriode = PeriodeDto(periode3.fom, periode3.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
        )
    }

    @Test
    fun `sammendrag - endret beløp etterfulgt av to uendrede perioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
                periode3 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1000.kroner,
                periode3 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode1.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            UendretPeriodeDto(periode2.fom, periode3.tom, 2000),
        )
    }

    @Test
    fun `sammendrag - to like perioder, et endret beløp`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = periode1.fom,
                tom = periode1.tom,
                gammelPeriode = PeriodeDto(periode1.fom, periode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            UendretPeriodeDto(periode2.fom, periode2.tom, 1000),
        )
    }
}
