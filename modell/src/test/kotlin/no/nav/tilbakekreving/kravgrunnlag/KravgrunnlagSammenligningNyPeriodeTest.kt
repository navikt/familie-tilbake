package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NyPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UendretPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class KravgrunnlagSammenligningNyPeriodeTest {
    @Test
    fun `flere perioder fører til ny periode forskjell`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val forskjeller = sammenlign(
            nåværende = listOf(periode to 2000.kroner),
            oppdatert = listOf(
                periode to 2000.kroner,
                (1.februar(2021) til 28.februar(2021)) to 2000.kroner,
            ),
        ).resultat()

        forskjeller.shouldBeSingle().shouldBeInstanceOf<KravgrunnlagSammenligning.Forskjell.NyPeriode>()
    }

    @Test
    fun `sammendrag - ny periode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val sammendrag = sammendrag(
            nåværende = listOf(periode to 2000.kroner),
            oppdatert = listOf(
                periode to 2000.kroner,
                nyPeriode to 2000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            UendretPeriodeDto(periode.fom, periode.tom, 2000),
            NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 2000),
        )
    }

    @Test
    fun `sammendrag - slår sammen nye perioder og beregner forskjell i eksisterende`() {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode1 = 1.februar(2021) til 28.februar(2021)
        val nyPeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(eksisterendePeriode to 1000.kroner),
            oppdatert = listOf(
                eksisterendePeriode to 1500.kroner,
                nyPeriode1 to 1000.kroner,
                nyPeriode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = eksisterendePeriode.fom,
                tom = eksisterendePeriode.tom,
                gammelPeriode = PeriodeDto(eksisterendePeriode.fom, eksisterendePeriode.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            NyPeriodeDto(
                fom = 1.februar(2021),
                tom = 31.mars(2021),
                beløp = 2000,
            ),
        )
    }

    @Test
    fun `sammendrag - nye perioder på hver sin side av eksisterende periode`() {
        val eksisterendePeriode = 1.februar(2021) til 28.februar(2021)
        val nyPeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(eksisterendePeriode to 1000.kroner),
            oppdatert = listOf(
                nyPeriode1 to 1000.kroner,
                eksisterendePeriode to 1000.kroner,
                nyPeriode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            NyPeriodeDto(nyPeriode1.fom, nyPeriode1.tom, 1000),
            UendretPeriodeDto(eksisterendePeriode.fom, eksisterendePeriode.tom, 1000),
            NyPeriodeDto(nyPeriode2.fom, nyPeriode2.tom, 1000),
        )
    }

    @Test
    fun `sammendrag - ny periode mellom to eksisterende perioder`() {
        val eksisterendePeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val eksisterendePeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                eksisterendePeriode1 to 1000.kroner,
                eksisterendePeriode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                eksisterendePeriode1 to 1500.kroner,
                nyPeriode to 1000.kroner,
                eksisterendePeriode2 to 1500.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = eksisterendePeriode1.fom,
                tom = eksisterendePeriode1.tom,
                gammelPeriode = PeriodeDto(eksisterendePeriode1.fom, eksisterendePeriode1.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 1000),
            EndretPeriodeDto(
                fom = eksisterendePeriode2.fom,
                tom = eksisterendePeriode2.tom,
                gammelPeriode = PeriodeDto(eksisterendePeriode2.fom, eksisterendePeriode2.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
        )
    }

    @Test
    fun `sammendrag - nye perioder med mellomrom`() {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode1 = 1.mars(2021) til 31.mars(2021)
        val nyPeriode2 = 1.mai(2021) til 31.mai(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(eksisterendePeriode to 1000.kroner),
            oppdatert = listOf(
                eksisterendePeriode to 1000.kroner,
                nyPeriode1 to 1000.kroner,
                nyPeriode2 to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            UendretPeriodeDto(eksisterendePeriode.fom, eksisterendePeriode.tom, 1000),
            NyPeriodeDto(
                fom = 1.mars(2021),
                tom = 31.mai(2021),
                beløp = 2000,
            ),
        )
    }

    @Test
    fun `sammendrag - ny periode og endret beløp inntil hverandre`() {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(eksisterendePeriode to 1000.kroner),
            oppdatert = listOf(
                eksisterendePeriode to 1500.kroner,
                nyPeriode to 1000.kroner,
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = eksisterendePeriode.fom,
                tom = eksisterendePeriode.tom,
                gammelPeriode = PeriodeDto(eksisterendePeriode.fom, eksisterendePeriode.tom),
                gammeltBeløp = 1000,
                nyttBeløp = 1500,
            ),
            NyPeriodeDto(
                fom = nyPeriode.fom,
                tom = nyPeriode.tom,
                beløp = 1000,
            ),
        )
    }

    @Test
    fun `sammendrag - ny periode og sammenslått endret beløp`() {
        val eksisterendePeriode1 = 1.januar(2021) til 31.januar(2021)
        val eksisterendePeriode2 = 1.februar(2021) til 28.februar(2021)
        val nyPeriode = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                eksisterendePeriode1 to 1000.kroner,
                eksisterendePeriode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                eksisterendePeriode1 to 1500.kroner,
                eksisterendePeriode2 to 1500.kroner,
                nyPeriode to 1000.kroner,
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
            NyPeriodeDto(
                fom = nyPeriode.fom,
                tom = nyPeriode.tom,
                beløp = 1000,
            ),
        )
    }
}
