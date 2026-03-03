package no.nav.tilbakekreving.beregning.delperiode

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DelperiodeTest {
    @Test
    fun `slår sammen foreldet perioder`() {
        val foreldelsesperiode = 1.januar(2021) til 31.mars(2021)
        val foreldelser = listOf(
            Foreldet.opprett(
                foreldelsesperiode,
                listOf(
                    KravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                    KravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                    KravgrunnlagPeriode(1.mars(2021) til 31.mars(2021)),
                ),
            ),
        )

        foreldelser.map { it.beregningsresultat() } shouldBe listOf(
            Beregningsresultatsperiode(
                periode = foreldelsesperiode,
                vurdering = AnnenVurdering.FORELDET,
                feilutbetaltBeløp = 6000.kroner,
                andelAvBeløp = 0.kroner,
                renteprosent = null,
                manueltSattTilbakekrevingsbeløp = null,
                tilbakekrevingsbeløpUtenRenter = 0.kroner,
                rentebeløp = 0.kroner,
                tilbakekrevingsbeløp = 0.kroner,
                skattebeløp = 0.kroner,
                tilbakekrevingsbeløpEtterSkatt = 0.kroner,
                utbetaltYtelsesbeløp = 60000.kroner,
                riktigYtelsesbeløp = 54000.kroner,
            ),
        )
    }

    class KravgrunnlagPeriode(private val periode: Datoperiode) : KravgrunnlagPeriodeAdapter {
        override fun periode(): Datoperiode = periode

        override fun feilutbetaltYtelsesbeløp(): BigDecimal = 2000.kroner

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> = listOf(
            object : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
                override fun tilbakekrevesBeløp(): BigDecimal = 2000.kroner

                override fun riktigYteslesbeløp(): BigDecimal = 18000.kroner

                override fun utbetaltYtelsesbeløp(): BigDecimal = 20000.kroner

                override fun skatteprosent(): BigDecimal = 0.prosent

                override fun klassekode(): String {
                    return "BATR"
                }
            },
        )
    }
}
