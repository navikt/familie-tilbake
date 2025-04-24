package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.inspectors.forAll
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class KravgrunnlagValidatorTest {
    @Test
    fun `helt standard kravgrunnlag`() {
        KravgrunnlagValidatorV2.valider(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                    periode(1.februar til 28.februar).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
        ).shouldBeInstanceOf<ValidationResult.Ok>()
    }

    @Test
    fun `kravgrunnlag uten referanse`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                referanse = null,
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Mangler referanse",
            ),
        )
    }

    @Test
    fun `månedsvalidator, flere ulike måneder`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(periode = 1.januar til 28.februar).medStandardFeilutbetaling(),
                    periode(periode = 1.mars til 30.april).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${28.februar} er ikke innenfor samme kalendermåned",
                "Perioden ${1.mars} til ${30.april} er ikke innenfor samme kalendermåned",
            ),
        )
    }

    @Test
    fun `månedsvalidator, en ulik måned`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 28.februar).medStandardFeilutbetaling(),
                    periode(1.mars til 31.mars).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${28.februar} er ikke innenfor samme kalendermåned",
            ),
        )
    }

    @Test
    fun `månedsvalidator, flere ulike feil fra en periode`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 27.februar).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${27.februar} er ikke innenfor samme kalendermåned",
                "Perioden ${1.januar} til ${27.februar} slutter ikke siste dag i måned",
            ),
        )
    }

    @Test
    fun `overlappende perioder`() {
        assertFeil(
            kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} overlapper med perioden ${1.januar} til ${31.januar}",
            ),
        )
    }

    @Test
    fun `overlappende perioder, ulik slutt og startdato`() {
        assertFeil(
            kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                    periode(30.januar til 28.februar).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} overlapper med perioden ${30.januar} til ${28.februar}",
                "Perioden ${30.januar} til ${28.februar} er ikke innenfor samme kalendermåned",
                "Perioden ${30.januar} til ${28.februar} starter ikke første dag i måned",
            ),
        )
    }

    @Test
    fun `overlappende perioder, feil rekkefølge`() {
        assertFeil(
            kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                    periode(1.februar til 28.februar).medStandardFeilutbetaling(),
                    periode(1.januar til 31.januar).medStandardFeilutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} overlapper med perioden ${1.januar} til ${31.januar}",
            ),
        )
    }

    @Test
    fun `lavere beløp på skatt enn beregnet`() {
        assertFeil(
            kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar, beløpSkattMåned = 299)
                        .medStandardFeilutbetaling(
                            tilbakekreves = 3000,
                            skatteprosent = 10,
                        ),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Maks skatt for perioden 2018-01-01 til 2018-01-31 er 299.00, men maks tilbakekreving ganget med skattesats blir 300",
            ),
        )
    }

    @Test
    fun `kravgrunnlag uten postering med klassetype feil`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medYtelsesutbetaling(),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} mangler postering med klassetype=FEIL",
                "Perioden ${1.januar} til ${31.januar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(3000.00) i forhold til summert beløpNy i FEIL postering(0.00)",
            ),
        )
    }

    @Test
    fun `kravgrunnlag uten postering med klassetype ytel`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(
                            skatteprosent = 10,
                            beløpTilbakekreves = 3000,
                        ),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} mangler postering med klassetype=YTEL",
                "Perioden ${1.januar} til ${31.januar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(0.00) i forhold til summert beløpNy i FEIL postering(3000.00)",
            ),
        )
    }

    @Test
    fun `negativt nytt beløp for postering med type YTEL`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(beløpNy = -1)
                        .medYtelsesutbetaling(beløpNy = 27000, originaltBeløp = 30000, beløpTilbakekreves = 3000),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} har feilpostering med negativt beløp",
                "Perioden ${1.januar} til ${31.januar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(3000.00) i forhold til summert beløpNy i FEIL postering(-1.00)",
            ),
        )
    }

    @Test
    fun `ulikt beløp i ytelsespostering og feilpostering`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(beløpNy = 1400)
                        .medYtelsesutbetaling(
                            originaltBeløp = 30000,
                            beløpNy = 28500,
                            beløpTilbakekreves = 1500,
                        ),
                    periode(1.februar til 28.februar)
                        .medFeilutbetaling(beløpNy = 1300)
                        .medYtelsesutbetaling(
                            originaltBeløp = 30000,
                            beløpNy = 28600,
                            beløpTilbakekreves = 1400,
                        ),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(1500.00) i forhold til summert beløpNy i FEIL postering(1400.00)",
                "Perioden ${1.februar} til ${28.februar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(1400.00) i forhold til summert beløpNy i FEIL postering(1300.00)",
            ),
        )
    }

    @Test
    fun `ulikt nytt beløp i feilpostering og beløp som tilbakekreves i ytelsespostering`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(
                            beløpNy = 3000,
                        )
                        .medYtelsesutbetaling(
                            beløpTilbakekreves = 2900,
                        ),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} har ulikt summert tilbakekrevingsbeløp i YTEL postering(2900.00) " +
                    "i forhold til summert beløpNy i FEIL postering(3000.00)",
            ),
        )
    }

    @Test
    fun `periode hvor differansen mellom opprinnelig beløp og beløp som tilbakekreves er større enn beløpTilbakekreves`() {
        assertFeil(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(beløpNy = 3100)
                        .medYtelsesutbetaling(beløpTilbakekreves = 3100),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Har en eller flere perioder med YTEL-postering med tilbakekrevesBeløp som er større enn differanse mellom nyttBeløp og opprinneligBeløp",
            ),
        )
    }

    @Test
    fun `perioder hvor differansen mellom opprinnelig beløp og beløp som tilbakekreves er større og mindre enn beløpTilbakekreves`() {
        KravgrunnlagValidatorV2.valider(
            kravgrunnlag = kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    periode(1.januar til 31.januar)
                        .medFeilutbetaling(beløpNy = 3100)
                        .medYtelsesutbetaling(beløpTilbakekreves = 3100),
                    periode(1.februar til 28.februar)
                        .medFeilutbetaling(beløpNy = 3100)
                        .medYtelsesutbetaling(beløpTilbakekreves = 3100),
                    periode(1.mars til 31.mars)
                        .medFeilutbetaling(beløpNy = 2900)
                        .medYtelsesutbetaling(beløpTilbakekreves = 2900),
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
        ).shouldBeInstanceOf<ValidationResult.Ok>()
    }

    private fun assertFeil(
        kravgrunnlag: DetaljertKravgrunnlagDto,
        periodeValidator: PeriodeValidator,
        forventetFeil: List<String>,
    ) {
        val valideringsresultat = KravgrunnlagValidatorV2.valider(kravgrunnlag, periodeValidator)
            .shouldBeInstanceOf<ValidationResult.Feil>()
        forventetFeil.forAll { forventetFeil ->
            valideringsresultat.failures.forOne {
                it.melding shouldBe forventetFeil
            }
        }
        valideringsresultat.failures.forAll {
            forventetFeil shouldContain it.melding
        }
    }

    private fun kravgrunnlag(
        referanse: String? = UUID.randomUUID().toString(),
        tilbakekrevingsperioder: List<DetaljertKravgrunnlagPeriodeDto> = listOf(
            periode(1.januar til 31.januar).medStandardFeilutbetaling(),
        ),
    ) = DetaljertKravgrunnlagDto().apply {
        this.kravgrunnlagId = BigInteger.ZERO
        this.referanse = referanse
        this.tilbakekrevingsPeriode.addAll(tilbakekrevingsperioder)
    }

    private fun periode(
        periode: Datoperiode,
        beløpSkattMåned: Int = STANDARD_UTBETALT_BELØP * STANDARD_SKATT_PROSENT / 100,
    ) = DetaljertKravgrunnlagPeriodeDto()
        .apply {
            this.belopSkattMnd = beløpSkattMåned.toBigDecimal().setScale(2)
            this.periode = PeriodeDto().apply {
                this.fom = periode.fom
                this.tom = periode.tom
            }
        }

    private fun DetaljertKravgrunnlagPeriodeDto.medStandardFeilutbetaling(
        tilbakekreves: Int = STANDARD_BELØP_TILBAKEKREVES,
        skatteprosent: Int = STANDARD_SKATT_PROSENT,
    ) = apply {
        medFeilutbetaling(
            skatteprosent = skatteprosent,
        )
        medYtelsesutbetaling(
            beløpTilbakekreves = tilbakekreves,
            skatteprosent = skatteprosent,
            beløpNy = 27000,
            originaltBeløp = 27000 + tilbakekreves,
        )
    }

    private fun DetaljertKravgrunnlagPeriodeDto.medFeilutbetaling(
        beløpTilbakekreves: Int = 0,
        beløpNy: Int = STANDARD_BELØP_TILBAKEKREVES,
        originaltBeløp: Int = 0,
        skatteprosent: Int = STANDARD_SKATT_PROSENT,
    ) = apply {
        this.tilbakekrevingsBelop.add(
            DetaljertKravgrunnlagBelopDto().apply {
                this.belopTilbakekreves = beløpTilbakekreves.toBigDecimal().setScale(2)
                this.skattProsent = skatteprosent.toBigDecimal()
                this.belopNy = beløpNy.toBigDecimal().setScale(2)
                this.belopOpprUtbet = originaltBeløp.toBigDecimal()
                this.typeKlasse = TypeKlasseDto.FEIL
            },
        )
    }

    private fun DetaljertKravgrunnlagPeriodeDto.medYtelsesutbetaling(
        beløpTilbakekreves: Int = STANDARD_BELØP_TILBAKEKREVES,
        beløpNy: Int = STANDARD_UTBETALT_BELØP - STANDARD_BELØP_TILBAKEKREVES,
        originaltBeløp: Int = STANDARD_UTBETALT_BELØP,
        skatteprosent: Int = STANDARD_SKATT_PROSENT,
    ) = apply {
        this.tilbakekrevingsBelop.add(
            DetaljertKravgrunnlagBelopDto().apply {
                this.belopTilbakekreves = beløpTilbakekreves.toBigDecimal().setScale(2)
                this.skattProsent = skatteprosent.toBigDecimal()
                this.belopNy = beløpNy.toBigDecimal().setScale(2)
                this.belopOpprUtbet = originaltBeløp.toBigDecimal()
                this.typeKlasse = TypeKlasseDto.YTEL
            },
        )
    }

    companion object {
        const val STANDARD_BELØP_TILBAKEKREVES = 3000
        const val STANDARD_SKATT_PROSENT = 10
        const val STANDARD_UTBETALT_BELØP = 30000
    }
}
