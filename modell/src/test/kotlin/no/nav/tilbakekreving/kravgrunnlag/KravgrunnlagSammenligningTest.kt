package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.feilutbetalteBeløp
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NyPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.test.mars
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private fun sammenlign(
    nåværende: List<KravgrunnlagHendelse.Periode>,
    oppdatert: List<KravgrunnlagHendelse.Periode>,
) = KravgrunnlagSammenligning(
    originaltKravgrunnlag = kravgrunnlag(perioder = nåværende),
    nyttKravgrunnlag = kravgrunnlag(perioder = oppdatert),
    sporing = Sporing("", ""),
).sammendrag()

private fun beløp(tilbakekrevesBeløp: BigDecimal) = ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp) + feilutbetalteBeløp(ytelsesbeløp(tilbakekrevesBeløp = tilbakekrevesBeløp))

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
    fun `flere perioder fører til ny periode forskjell`() {
        val forskjeller = KravgrunnlagSammenligning(
            originaltKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)))),
            nyttKravgrunnlag = kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                    kravgrunnlagPeriode(periode = 1.februar(2021) til 28.februar(2021)),
                ),
            ),
            sporing = Sporing("", ""),
        ).resultat()

        forskjeller.shouldBeSingle().shouldBeInstanceOf<KravgrunnlagSammenligning.Forskjell.NyPeriode>()
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

        forskjell.gammeltBeløp shouldBe 1000.kroner
        forskjell.nyttBeløp shouldBe 2000.kroner
    }

    @Test
    fun `sammendrag - ingen endringer gir tomt resultat`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val sammendrag = sammenlign(
            nåværende = listOf(kravgrunnlagPeriode(periode = periode)),
            oppdatert = listOf(kravgrunnlagPeriode(periode = periode)),
        )

        sammendrag.shouldBeEmpty()
    }

    @Test
    fun `sammendrag - ny periode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val sammendrag = sammenlign(
            nåværende = listOf(kravgrunnlagPeriode(periode = periode)),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode),
                kravgrunnlagPeriode(periode = nyPeriode),
            ),
        )

        sammendrag shouldBe listOf(NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 2000))
    }

    @Test
    fun `sammendrag - endring av beløp for tre perioder slås sammen`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode3, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode3, ytelsesbeløp = beløp(1500.kroner)),
            ),
        )

        sammendrag shouldBe listOf(
            EndretPeriodeDto(
                fom = 1.januar(2021),
                tom = 31.mars(2021),
                gammelPeriode = PeriodeDto(1.januar(2021), 31.mars(2021)),
                gammeltBeløp = 3000,
                nyttBeløp = 4000,
            ),
        )
    }

    @Test
    fun `sammendrag - endring av beløp for flere perioder slås sammen`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1500.kroner)),
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
    fun `sammendrag - slår sammen nye perioder og beregner forskjell i eksisterende`() {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode1 = 1.februar(2021) til 28.februar(2021)
        val nyPeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode2, ytelsesbeløp = beløp(1000.kroner)),
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
    fun `sammendrag - endring av beløp med gap mellom periodene slås sammen`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1500.kroner)),
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

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode, ytelsesbeløp = beløp(2000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode, ytelsesbeløp = beløp(1000.kroner)),
            ),
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
    fun `sammendrag - endret beløp over flere perioder utligner hverandre`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(2000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(2000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
        )

        sammendrag.shouldBeEmpty()
    }

    @Test
    fun `sammendrag - to like perioder, et endret beløp`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = periode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = periode2, ytelsesbeløp = beløp(1000.kroner)),
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
        )
    }

    @Test
    fun `sammendrag - nye perioder på hver sin side av eksisterende periode`() {
        val eksisterendePeriode = 1.februar(2021) til 28.februar(2021)
        val nyPeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = nyPeriode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
        )

        sammendrag shouldBe listOf(
            NyPeriodeDto(nyPeriode1.fom, nyPeriode1.tom, 1000),
            NyPeriodeDto(nyPeriode2.fom, nyPeriode2.tom, 1000),
        )
    }

    @Test
    fun `sammendrag - ny periode mellom to eksisterende perioder`() {
        val eksisterendePeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val eksisterendePeriode2 = 1.mars(2021) til 31.mars(2021)

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = eksisterendePeriode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = eksisterendePeriode2, ytelsesbeløp = beløp(1500.kroner)),
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

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
        )

        sammendrag shouldBe listOf(
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

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode, ytelsesbeløp = beløp(1000.kroner)),
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

        val sammendrag = sammenlign(
            nåværende = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode1, ytelsesbeløp = beløp(1000.kroner)),
                kravgrunnlagPeriode(periode = eksisterendePeriode2, ytelsesbeløp = beløp(1000.kroner)),
            ),
            oppdatert = listOf(
                kravgrunnlagPeriode(periode = eksisterendePeriode1, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = eksisterendePeriode2, ytelsesbeløp = beløp(1500.kroner)),
                kravgrunnlagPeriode(periode = nyPeriode, ytelsesbeløp = beløp(1000.kroner)),
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
