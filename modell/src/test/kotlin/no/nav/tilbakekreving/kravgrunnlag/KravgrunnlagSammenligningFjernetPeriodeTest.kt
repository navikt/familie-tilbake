package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FjernetPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NyPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UendretPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class KravgrunnlagSammenligningFjernetPeriodeTest {
    @Test
    fun `sammendrag - fjernet periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(periode1 to 1000.kroner),
        )

        sammendrag shouldBe listOf(
            UendretPeriodeDto(periode1.fom, periode1.tom, 1000),
            FjernetPeriodeDto(periode2.fom, periode2.tom, 1000),
        )
    }

    @Test
    fun `sammendrag - fjernet første periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(periode2 to 1000.kroner),
        )

        sammendrag shouldBe listOf(
            FjernetPeriodeDto(periode1.fom, periode1.tom, 1000),
            UendretPeriodeDto(periode2.fom, periode2.tom, 1000),
        )
    }

    @Test
    fun `sammendrag - flere sammenhengende fjernede perioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
                periode3 to 1000.kroner,
            ),
            oppdatert = listOf(periode1 to 1000.kroner),
        )

        sammendrag shouldBe listOf(
            UendretPeriodeDto(periode1.fom, periode1.tom, 1000),
            FjernetPeriodeDto(periode2.fom, periode3.tom, 2000),
        )
    }

    @Test
    fun `sammendrag - fjernede perioder på hver sin side av gjenværende periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
                periode3 to 1000.kroner,
            ),
            oppdatert = listOf(periode2 to 1000.kroner),
        )

        sammendrag shouldBe listOf(
            FjernetPeriodeDto(periode1.fom, periode1.tom, 1000),
            UendretPeriodeDto(periode2.fom, periode2.tom, 1000),
            FjernetPeriodeDto(periode3.fom, periode3.tom, 1000),
        )
    }

    @Test
    fun `sammendrag - blanding av endret, fjernet og ny periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val nyPeriode = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 1000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                nyPeriode to 1000.kroner,
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
            FjernetPeriodeDto(periode2.fom, periode2.tom, 1000),
            NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 1000),
        )
    }
}
