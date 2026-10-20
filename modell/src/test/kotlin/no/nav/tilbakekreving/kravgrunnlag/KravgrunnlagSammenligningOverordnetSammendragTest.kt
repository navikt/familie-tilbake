package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class KravgrunnlagSammenligningOverordnetSammendragTest {
    @Test
    fun `overordnet sammendrag - ingen endring`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        overordnetSammendrag(
            nåværende = listOf(periode to 1000.kroner),
            oppdatert = listOf(periode to 1000.kroner),
        ) shouldBe null
    }

    @Test
    fun `overordnet sammendrag - endret beløp`() {
        val periode = 1.januar(2021) til 31.januar(2021)

        val sammendrag = overordnetSammendrag(
            nåværende = listOf(periode to 1000.kroner),
            oppdatert = listOf(periode to 1500.kroner),
        ).shouldNotBeNull()

        sammendrag.fom shouldBe 1.januar(2021)
        sammendrag.tom shouldBe 31.januar(2021)
        sammendrag.gammeltBeløp.compareTo(1000.kroner) shouldBe 0
        sammendrag.nyttBeløp.compareTo(1500.kroner) shouldBe 0
    }

    @Test
    fun `overordnet sammendrag - endret beløp i flere perioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode3 = 1.mars(2021) til 31.mars(2021)
        val sammendrag = overordnetSammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode3 to 2000.kroner,
            ),
            oppdatert = listOf(
                periode1 to 1500.kroner,
                periode3 to 2500.kroner,
            ),
        ).shouldNotBeNull()

        sammendrag.fom shouldBe 1.januar(2021)
        sammendrag.tom shouldBe 31.mars(2021)
        sammendrag.gammeltBeløp.compareTo(3000.kroner) shouldBe 0
        sammendrag.nyttBeløp.compareTo(4000.kroner) shouldBe 0
    }

    @Test
    fun `overordnet sammendrag - ny periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = overordnetSammendrag(
            nåværende = listOf(periode1 to 1000.kroner),
            oppdatert = listOf(
                periode1 to 1000.kroner,
                periode2 to 2000.kroner,
            ),
        ).shouldNotBeNull()

        sammendrag.fom shouldBe 1.februar(2021)
        sammendrag.tom shouldBe 28.februar(2021)
        sammendrag.gammeltBeløp.compareTo(BigDecimal.ZERO) shouldBe 0
        sammendrag.nyttBeløp.compareTo(2000.kroner) shouldBe 0
    }

    @Test
    fun `overordnet sammendrag - fjernet periode`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val sammendrag = overordnetSammendrag(
            nåværende = listOf(
                periode1 to 1000.kroner,
                periode2 to 2000.kroner,
            ),
            oppdatert = listOf(periode1 to 1000.kroner),
        ).shouldNotBeNull()

        sammendrag.fom shouldBe 1.februar(2021)
        sammendrag.tom shouldBe 28.februar(2021)
        sammendrag.gammeltBeløp.compareTo(2000.kroner) shouldBe 0
        sammendrag.nyttBeløp.compareTo(BigDecimal.ZERO) shouldBe 0
    }

    private fun overordnetSammendrag(
        nåværende: List<Pair<Datoperiode, BigDecimal>>,
        oppdatert: List<Pair<Datoperiode, BigDecimal>>,
    ) = KravgrunnlagSammenligning(
        originaltKravgrunnlag = kravgrunnlag(perioder = nåværende.tilKravgrunnlagPerioder()),
        nyttKravgrunnlag = kravgrunnlag(perioder = oppdatert.tilKravgrunnlagPerioder()),
        sporing = Sporing("", ""),
    ).let {
        var resultat: KravgrunnlagSammenligning.OverordnetSammendrag? = null
        it.oppdaterSteg(
            listOf(
                object : EndretKravgrunnlagObservatør {
                    override fun nyttKravgrunnlagMottatt(sammendrag: KravgrunnlagSammenligning.OverordnetSammendrag) {
                        resultat = sammendrag
                    }
                },
            ),
        )
        resultat
    }
}
