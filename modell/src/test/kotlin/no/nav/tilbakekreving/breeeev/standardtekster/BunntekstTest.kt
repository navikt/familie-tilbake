package no.nav.tilbakekreving.breeeev.standardtekster

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst.PERSONVÆRN_ERKLÆRING
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst.RETT_TIL_HJELP
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst.RETT_TIL_INNSYN
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst.SPØRSMÅL
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class BunntekstTest {
    @Test
    fun standardtekster() {
        Bunntekst.STANDARD_BUNNTEKSTER shouldBe arrayOf(
            RETT_TIL_INNSYN,
            PERSONVÆRN_ERKLÆRING,
            RETT_TIL_HJELP,
            SPØRSMÅL,
        )
    }

    @MethodSource("bunntekster")
    @ParameterizedTest
    fun `velger riktig bunntekster basert på beregning og ytelse`(
        beregningsresultat: Beregningsresultat,
        ytelse: Ytelse,
        forventet: Set<Bunntekst>,
    ) {
        Bunntekst.finnTekster(beregningsresultat, ytelse) shouldBe forventet
    }

    companion object {
        @JvmStatic
        fun bunntekster(): List<Arguments> {
            return listOf(
                testdata(
                    name = "Ingen tilbakekreving",
                    vedtaksresultat = Vedtaksresultat.INGEN_TILBAKEBETALING,
                    renter = false,
                    ytelse = Ytelse.Tilleggsstønad,
                    setOf(*Bunntekst.STANDARD_BUNNTEKSTER),
                ),
                testdata(
                    name = "Full tilbakekreving, ingen skatt eller renter",
                    vedtaksresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    renter = false,
                    ytelse = Ytelse.Tilleggsstønad,
                    setOf(
                        Bunntekst.HVORDAN_BETALE_TILBAKE,
                        Bunntekst.RETT_TIL_Å_KLAGE,
                        *Bunntekst.STANDARD_BUNNTEKSTER,
                    ),
                ),
                testdata(
                    name = "Delvis tilbakekreving, ingen skatt eller renter",
                    vedtaksresultat = Vedtaksresultat.DELVIS_TILBAKEBETALING,
                    renter = false,
                    ytelse = Ytelse.Tilleggsstønad,
                    setOf(
                        Bunntekst.HVORDAN_BETALE_TILBAKE,
                        Bunntekst.RETT_TIL_Å_KLAGE,
                        *Bunntekst.STANDARD_BUNNTEKSTER,
                    ),
                ),
                testdata(
                    name = "Full tilbakekreving, ingen skatt, med renter",
                    vedtaksresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    renter = true,
                    ytelse = Ytelse.Tilleggsstønad,
                    setOf(
                        Bunntekst.RENTER,
                        Bunntekst.HVORDAN_BETALE_TILBAKE,
                        Bunntekst.RETT_TIL_Å_KLAGE,
                        *Bunntekst.STANDARD_BUNNTEKSTER,
                    ),
                ),
                testdata(
                    name = "Full tilbakekreving, skatt, ingen renter",
                    vedtaksresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    renter = true,
                    ytelse = Ytelse.Arbeidsavklaringspenger,
                    setOf(
                        Bunntekst.RENTER,
                        Bunntekst.SKATT,
                        Bunntekst.HVORDAN_BETALE_TILBAKE,
                        Bunntekst.RETT_TIL_Å_KLAGE,
                        *Bunntekst.STANDARD_BUNNTEKSTER,
                    ),
                ),
                testdata(
                    name = "Full tilbakekreving, ingen skatt, med renter",
                    vedtaksresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    renter = true,
                    ytelse = Ytelse.Tilleggsstønad,
                    setOf(
                        Bunntekst.RENTER,
                        Bunntekst.HVORDAN_BETALE_TILBAKE,
                        Bunntekst.RETT_TIL_Å_KLAGE,
                        *Bunntekst.STANDARD_BUNNTEKSTER,
                    ),
                ),
            )
        }

        private fun testdata(
            name: String,
            vedtaksresultat: Vedtaksresultat,
            renter: Boolean,
            ytelse: Ytelse,
            expected: Set<Bunntekst>,
        ): Arguments {
            return Arguments.argumentSet(
                name,
                Beregningsresultat(
                    beregningsresultatsperioder = listOf(
                        Beregningsresultatsperiode(
                            periode = 1.januar(2021) til 31.januar(2021),
                            vurdering = Aktsomhet.GROV_UAKTSOMHET,
                            feilutbetaltBeløp = 5000.kroner,
                            andelAvBeløp = 100.prosent,
                            renteprosent = 10.prosent.takeIf { renter },
                            manueltSattTilbakekrevingsbeløp = null,
                            tilbakekrevingsbeløpUtenRenter = 5000.kroner,
                            rentebeløp = if (renter) {
                                500.kroner
                            } else {
                                0.kroner
                            },
                            tilbakekrevingsbeløp = 5000.kroner,
                            skattebeløp = if (ytelse.beregnerSkatt) {
                                50.kroner
                            } else {
                                0.kroner
                            },
                            tilbakekrevingsbeløpEtterSkatt = 5000.kroner,
                            utbetaltYtelsesbeløp = 5000.kroner,
                            riktigYtelsesbeløp = 0.kroner,
                        ),
                    ),
                    vedtaksresultat = vedtaksresultat,
                ),
                ytelse,
                expected,
            )
        }
    }
}
