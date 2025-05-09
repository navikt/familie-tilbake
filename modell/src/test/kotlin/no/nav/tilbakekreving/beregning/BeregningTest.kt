package no.nav.tilbakekreving.beregning

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.medTilbakekrevesBeløp
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.mars
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class BeregningTest {
    @Test
    fun `fordeler enkelt beløp`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 31.januar burdeForstått medForsett(ileggesRenter = false),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1500.kroner,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.FORSETT,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1500.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1500.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1500.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
            ),
            Vedtaksresultat.FULL_TILBAKEBETALING,
        )
    }

    @Test
    fun `skatt pårvirkes ikke av renter`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 31.januar burdeForstått medForsett(ileggesRenter = true),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1500.kroner medSkatteprosent 50.prosent,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.FORSETT,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = 10.prosent,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1500.kroner,
                    rentebeløp = 150.kroner,
                    tilbakekrevingsbeløp = 1650.kroner,
                    skattebeløp = 750.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 900.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
            ),
            Vedtaksresultat.FULL_TILBAKEBETALING,
        )
    }

    @Test
    fun `fordeler manuelt beløp over flere perioder`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 28.februar godTro medBeløpIBehold(beløp = 1999.kroner),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1500.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 1500.kroner,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = null,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = 999.50.kroner,
                    tilbakekrevingsbeløpUtenRenter = 1000.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1000.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1000.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = null,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = 999.50.kroner,
                    tilbakekrevingsbeløpUtenRenter = 999.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 999.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 999.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
            ),
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
        )
    }

    @Test
    fun `beløp ikke i behold`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 28.februar godTro utenBeløpIBehold(),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1500.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 1500.kroner,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = 0.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 0.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 0.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 0.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 1500.kroner,
                    andelAvBeløp = 0.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 0.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 0.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 0.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18500.kroner,
                ),
            ),
            Vedtaksresultat.INGEN_TILBAKEBETALING,
        )
    }

    @Test
    fun `fordeler krone ut over to perioder`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 28.februar burdeForstått medSimpelUaktsomhet(prosentdel = 50.prosent),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1499.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 1499.kroner,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 1499.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 750.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 750.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 750.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18501.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 1499.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 749.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 749.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 749.kroner,
                    utbetaltYtelsesbeløp = 20000.kroner,
                    riktigYtelsesbeløp = 18501.kroner,
                ),
            ),
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
        )
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i separate vilkårsperioder med 100 prosent tilbakekreving og renter skal skal avrunde hver renteperiode ned`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 31.mars burdeForstått medGrovUaktsomhet(ileggesRenter = true),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 18609.kroner medSkatteprosent 50.prosent medOriginaltUtbetaltBeløp 44093.kroner medRiktigYtelsesbeløp 25484.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 18609.kroner medSkatteprosent 50.prosent medOriginaltUtbetaltBeløp 44093.kroner medRiktigYtelsesbeløp 25484.kroner,
                1.mars til 31.mars medTilbakekrevesBeløp 18609.kroner medSkatteprosent 50.prosent medOriginaltUtbetaltBeløp 44093.kroner medRiktigYtelsesbeløp 25484.kroner,
            ),
        )
        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.GROV_UAKTSOMHET,
                    feilutbetaltBeløp = 18609.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = 10.prosent,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 18609.kroner,
                    rentebeløp = 1861.kroner,
                    tilbakekrevingsbeløp = 20470.kroner,
                    skattebeløp = 9305.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 11165.kroner,
                    utbetaltYtelsesbeløp = 44093.kroner,
                    riktigYtelsesbeløp = 25484.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = Aktsomhet.GROV_UAKTSOMHET,
                    feilutbetaltBeløp = 18609.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = 10.prosent,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 18609.kroner,
                    rentebeløp = 1861.kroner,
                    tilbakekrevingsbeløp = 20470.kroner,
                    skattebeløp = 9304.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 11166.kroner,
                    utbetaltYtelsesbeløp = 44093.kroner,
                    riktigYtelsesbeløp = 25484.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.mars til 31.mars,
                    vurdering = Aktsomhet.GROV_UAKTSOMHET,
                    feilutbetaltBeløp = 18609.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = 10.prosent,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 18609.kroner,
                    rentebeløp = 1860.kroner,
                    tilbakekrevingsbeløp = 20469.kroner,
                    skattebeløp = 9304.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 11165.kroner,
                    utbetaltYtelsesbeløp = 44093.kroner,
                    riktigYtelsesbeløp = 25484.kroner,
                ),
            ),
            Vedtaksresultat.FULL_TILBAKEBETALING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med 50 prosent tilbakekreving og skatt avrunding`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.januar til 28.februar burdeForstått medSimpelUaktsomhet(prosentdel = 50.prosent),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1755.kroner medSkatteprosent 44.prosent medOriginaltUtbetaltBeløp 19950.kroner medRiktigYtelsesbeløp 18195.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 1755.kroner medSkatteprosent 50.prosent medOriginaltUtbetaltBeløp 19950.kroner medRiktigYtelsesbeløp 18195.kroner,
            ),
        )

        beregning.beregn() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 1755.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 878.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 878.kroner,
                    skattebeløp = 386.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 492.kroner,
                    utbetaltYtelsesbeløp = 19950.kroner,
                    riktigYtelsesbeløp = 18195.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 1755.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 877.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 877.kroner,
                    skattebeløp = 438.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 439.kroner,
                    utbetaltYtelsesbeløp = 19950.kroner,
                    riktigYtelsesbeløp = 18195.kroner,
                ),
            ),
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
        )
    }

    fun perioder(
        vararg perioder: TestKravgrunnlagPeriode,
    ) = object : KravgrunnlagAdapter {
        override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
            return perioder.toList()
        }
    }

    fun vurdering(
        vararg perioder: Vilkårsvurderingsteg.Vilkårsvurderingsperiode,
    ) = object : VilkårsvurderingAdapter {
        override fun perioder(): Set<VilkårsvurdertPeriodeAdapter> {
            return perioder.toSet()
        }
    }

    infix fun Datoperiode.godTro(beløpIBehold: Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold) = Vilkårsvurderingsteg.Vilkårsvurderingsperiode(
        id = UUID.randomUUID(),
        periode = this,
        begrunnelseForTilbakekreving = "",
        _vurdering = Vilkårsvurderingsteg.Vurdering.GodTro(
            beløpIBehold = beløpIBehold,
            begrunnelse = "",
        ),
    )

    infix fun Datoperiode.burdeForstått(aktsomhet: Vilkårsvurderingsteg.VurdertAktsomhet) = Vilkårsvurderingsteg.Vilkårsvurderingsperiode(
        id = UUID.randomUUID(),
        periode = this,
        begrunnelseForTilbakekreving = "",
        _vurdering = Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått(
            "",
            aktsomhet,
        ),
    )

    fun medForsett(ileggesRenter: Boolean): Vilkårsvurderingsteg.VurdertAktsomhet.Forsett {
        return Vilkårsvurderingsteg.VurdertAktsomhet.Forsett("", ileggesRenter)
    }

    fun medSimpelUaktsomhet(prosentdel: BigDecimal) = Vilkårsvurderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
        "",
        Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner("", emptySet()),
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(prosentdel.toInt()),
    )

    fun medGrovUaktsomhet(ileggesRenter: Boolean) = Vilkårsvurderingsteg.VurdertAktsomhet.GrovUaktsomhet(
        "",
        Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner("", emptySet()),
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei,
        skalIleggesRenter = ileggesRenter,
    )

    fun medBeløpIBehold(beløp: BigDecimal) = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(beløp)

    fun utenBeløpIBehold() = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei

    class TestKravgrunnlagPeriode(
        private val periode: Datoperiode,
        private val tilbakekrevesBeløp: BigDecimal,
        private var originaltUtbetaltBeløp: BigDecimal = 20000.kroner,
        private var skatteprosent: BigDecimal = 0.prosent,
    ) : KravgrunnlagPeriodeAdapter {
        private var riktigYteslesbeløp: BigDecimal? = null

        infix fun medOriginaltUtbetaltBeløp(beløp: BigDecimal): TestKravgrunnlagPeriode {
            originaltUtbetaltBeløp = beløp
            return this
        }

        infix fun medSkatteprosent(prosentdel: BigDecimal): TestKravgrunnlagPeriode {
            skatteprosent = prosentdel
            return this
        }

        infix fun medRiktigYtelsesbeløp(beløp: BigDecimal): TestKravgrunnlagPeriode {
            riktigYteslesbeløp = beløp
            return this
        }

        override fun periode(): Datoperiode = periode

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return tilbakekrevesBeløp
        }

        override fun utbetaltYtelsesbeløp(): BigDecimal {
            return originaltUtbetaltBeløp
        }

        override fun riktigYteslesbeløp(): BigDecimal {
            return riktigYteslesbeløp ?: (originaltUtbetaltBeløp - feilutbetaltYtelsesbeløp())
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return listOf(
                object : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
                    override fun beløp(): BigDecimal {
                        return tilbakekrevesBeløp
                    }

                    override fun skatteprosent(): BigDecimal {
                        return skatteprosent
                    }
                },
            )
        }

        companion object {
            infix fun Datoperiode.medTilbakekrevesBeløp(tilbakekrevesBeløp: BigDecimal): TestKravgrunnlagPeriode {
                return TestKravgrunnlagPeriode(this, tilbakekrevesBeløp)
            }

            val Int.kroner get() = BigDecimal(this)
            val Double.kroner get() = BigDecimal(this).setScale(2)
            val Int.prosent get() = BigDecimal(this)
        }
    }
}
