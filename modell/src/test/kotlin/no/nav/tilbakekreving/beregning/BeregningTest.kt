package no.nav.tilbakekreving.beregning

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.medTilbakekrevesBeløp
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
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

        val delperioder = beregning.beregn()
        delperioder shouldHaveSize 1
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 1500.kroner,
            tilbakekrevesBruttoMedRenter = 1500.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )
        beregning.oppsummer() shouldBe Beregningsresultat(
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
    fun `skatt påvirkes ikke av renter`() {
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

        val delperioder = beregning.beregn()
        delperioder shouldHaveSize 1
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 150.kroner,
            tilbakekrevesBrutto = 1500.kroner,
            tilbakekrevesBruttoMedRenter = 1650.kroner,
            skatt = 750.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )
        beregning.oppsummer() shouldBe Beregningsresultat(
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

        val delperiode = beregning.beregn()
        delperiode[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 1000.kroner,
            tilbakekrevesBruttoMedRenter = 1000.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )
        delperiode[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 999.kroner,
            tilbakekrevesBruttoMedRenter = 999.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )

        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 28.februar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 3000.kroner,
                    andelAvBeløp = null,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = 1999.0.kroner,
                    tilbakekrevingsbeløpUtenRenter = 1999.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1999.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1999.kroner,
                    utbetaltYtelsesbeløp = 40000.kroner,
                    riktigYtelsesbeløp = 37000.kroner,
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

        val delperioder = beregning.beregn()
        delperioder shouldHaveSize 2
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 0.kroner,
            tilbakekrevesBruttoMedRenter = 0.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )
        delperioder[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 0.kroner,
            tilbakekrevesBruttoMedRenter = 0.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1500.kroner,
        )
        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 28.februar,
                    vurdering = AnnenVurdering.GOD_TRO,
                    feilutbetaltBeløp = 3000.kroner,
                    andelAvBeløp = 0.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 0.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 0.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 0.kroner,
                    utbetaltYtelsesbeløp = 40000.kroner,
                    riktigYtelsesbeløp = 37000.kroner,
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

        val delperioder = beregning.beregn()
        delperioder shouldHaveSize 2
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 750.kroner,
            tilbakekrevesBruttoMedRenter = 750.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1499.kroner,
        )
        delperioder[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 749.kroner,
            tilbakekrevesBruttoMedRenter = 749.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 20000.kroner,
            feilutbetaltBeløp = 1499.kroner,
        )
        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 28.februar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 2998.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1499.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1499.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1499.kroner,
                    utbetaltYtelsesbeløp = 40000.kroner,
                    riktigYtelsesbeløp = 37002.kroner,
                ),
            ),
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
        )
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i separate vilkårsperioder med 100 prosent tilbakekreving og renter skal avrunde hver renteperiode ned`() {
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

        val delperioder = beregning.beregn()
        delperioder shouldHaveSize 3
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 1861.kroner,
            tilbakekrevesBrutto = 18609.kroner,
            tilbakekrevesBruttoMedRenter = 20470.kroner,
            skatt = 9305.kroner,
            utbetaltYtelsesbeløp = 44093.kroner,
            feilutbetaltBeløp = 18609.kroner,
        )
        delperioder[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 1861.kroner,
            tilbakekrevesBrutto = 18609.kroner,
            tilbakekrevesBruttoMedRenter = 20470.kroner,
            skatt = 9304.kroner,
            utbetaltYtelsesbeløp = 44093.kroner,
            feilutbetaltBeløp = 18609.kroner,
        )
        delperioder[2].shouldMatch(
            periode = 1.mars til 31.mars,
            renter = 1860.kroner,
            tilbakekrevesBrutto = 18609.kroner,
            tilbakekrevesBruttoMedRenter = 20469.kroner,
            skatt = 9304.kroner,
            utbetaltYtelsesbeløp = 44093.kroner,
            feilutbetaltBeløp = 18609.kroner,
        )

        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.mars,
                    vurdering = Aktsomhet.GROV_UAKTSOMHET,
                    feilutbetaltBeløp = 55827.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = 10.prosent,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 55827.kroner,
                    rentebeløp = 5582.kroner,
                    tilbakekrevingsbeløp = 61409.kroner,
                    skattebeløp = 27913.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 33496.kroner,
                    utbetaltYtelsesbeløp = 132279.kroner,
                    riktigYtelsesbeløp = 76452.kroner,
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

        val delperioder = beregning.beregn()
        delperioder.size shouldBe 2
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 878.kroner,
            tilbakekrevesBruttoMedRenter = 878.kroner,
            skatt = 386.kroner,
            utbetaltYtelsesbeløp = 19950.kroner,
            feilutbetaltBeløp = 1755.kroner,
        )
        delperioder[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 877.kroner,
            tilbakekrevesBruttoMedRenter = 877.kroner,
            skatt = 438.kroner,
            utbetaltYtelsesbeløp = 19950.kroner,
            feilutbetaltBeløp = 1755.kroner,
        )
        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 28.februar,
                    vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                    feilutbetaltBeløp = 3510.kroner,
                    andelAvBeløp = 50.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1755.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1755.kroner,
                    skattebeløp = 824.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 931.kroner,
                    utbetaltYtelsesbeløp = 39900.kroner,
                    riktigYtelsesbeløp = 36390.kroner,
                ),
            ),
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
        )
    }

    @Test
    fun `perioder ute av rekkefølge`() {
        val beregning = Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = false,
            vilkårsvurdering = vurdering(
                1.februar til 28.februar burdeForstått medForsett(ileggesRenter = false),
                1.januar til 31.januar burdeForstått medForsett(ileggesRenter = false),
            ),
            foreldetPerioder = emptyList(),
            kravgrunnlag = perioder(
                1.januar til 31.januar medTilbakekrevesBeløp 1755.kroner medOriginaltUtbetaltBeløp 19950.kroner medRiktigYtelsesbeløp 18195.kroner,
                1.februar til 28.februar medTilbakekrevesBeløp 1755.kroner medOriginaltUtbetaltBeløp 19950.kroner medRiktigYtelsesbeløp 18195.kroner,
            ),
        )

        val delperioder = beregning.beregn()
        delperioder.size shouldBe 2
        delperioder[0].shouldMatch(
            periode = 1.januar til 31.januar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 1755.kroner,
            tilbakekrevesBruttoMedRenter = 1755.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 19950.kroner,
            feilutbetaltBeløp = 1755.kroner,
        )
        delperioder[1].shouldMatch(
            periode = 1.februar til 28.februar,
            renter = 0.kroner,
            tilbakekrevesBrutto = 1755.kroner,
            tilbakekrevesBruttoMedRenter = 1755.kroner,
            skatt = 0.kroner,
            utbetaltYtelsesbeløp = 19950.kroner,
            feilutbetaltBeløp = 1755.kroner,
        )

        beregning.oppsummer() shouldBe Beregningsresultat(
            listOf(
                Beregningsresultatsperiode(
                    periode = 1.januar til 31.januar,
                    vurdering = Aktsomhet.FORSETT,
                    feilutbetaltBeløp = 1755.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1755.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1755.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1755.kroner,
                    utbetaltYtelsesbeløp = 19950.kroner,
                    riktigYtelsesbeløp = 18195.kroner,
                ),
                Beregningsresultatsperiode(
                    periode = 1.februar til 28.februar,
                    vurdering = Aktsomhet.FORSETT,
                    feilutbetaltBeløp = 1755.kroner,
                    andelAvBeløp = 100.prosent,
                    renteprosent = null,
                    manueltSattTilbakekrevingsbeløp = null,
                    tilbakekrevingsbeløpUtenRenter = 1755.kroner,
                    rentebeløp = 0.kroner,
                    tilbakekrevingsbeløp = 1755.kroner,
                    skattebeløp = 0.kroner,
                    tilbakekrevingsbeløpEtterSkatt = 1755.kroner,
                    utbetaltYtelsesbeløp = 19950.kroner,
                    riktigYtelsesbeløp = 18195.kroner,
                ),
            ),
            Vedtaksresultat.FULL_TILBAKEBETALING,
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

    fun Delperiode.shouldMatch(
        periode: Datoperiode,
        renter: BigDecimal,
        tilbakekrevesBrutto: BigDecimal,
        tilbakekrevesBruttoMedRenter: BigDecimal,
        skatt: BigDecimal,
        utbetaltYtelsesbeløp: BigDecimal,
        feilutbetaltBeløp: BigDecimal,
    ) {
        this.periode shouldBe periode
        this.renter() shouldBe renter
        this.tilbakekrevesBrutto() shouldBe tilbakekrevesBrutto
        this.tilbakekrevesBruttoMedRenter() shouldBe tilbakekrevesBruttoMedRenter
        this.skatt() shouldBe skatt
        this.andel.utbetaltYtelsesbeløp() shouldBe utbetaltYtelsesbeløp
        this.andel.feilutbetaltBeløp() shouldBe feilutbetaltBeløp
    }

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
