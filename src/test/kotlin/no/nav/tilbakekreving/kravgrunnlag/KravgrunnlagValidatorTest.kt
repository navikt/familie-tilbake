package no.nav.tilbakekreving.kravgrunnlag

import io.kotest.inspectors.forAll
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

class KravgrunnlagValidatorTest {
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
                    1.januar til 28.februar,
                    1.mars til 30.april,
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
                    1.januar til 28.februar,
                    1.mars til 31.mars,
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
                    1.januar til 27.februar,
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
                    1.januar til 31.januar,
                    1.januar til 31.januar,
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
                    1.januar til 31.januar,
                    30.januar til 28.februar,
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} overlapper med perioden ${30.januar} til ${28.februar}",
            ),
        )
    }

    @Test
    fun `overlappende perioder, feil rekkefølge`() {
        assertFeil(
            kravgrunnlag(
                tilbakekrevingsperioder = listOf(
                    1.januar til 31.januar,
                    1.februar til 28.februar,
                    1.januar til 31.januar,
                ),
            ),
            periodeValidator = PeriodeValidator.MånedsperiodeValidator,
            forventetFeil = listOf(
                "Perioden ${1.januar} til ${31.januar} overlapper med perioden ${1.januar} til ${31.januar}",
            ),
        )
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
    }

    private fun kravgrunnlag(
        referanse: String? = UUID.randomUUID().toString(),
        tilbakekrevingsperioder: List<Datoperiode> = listOf(1.januar til 31.januar),
    ) = DetaljertKravgrunnlagDto().apply {
        this.kravgrunnlagId = BigInteger.ZERO
        this.referanse = referanse
        this.tilbakekrevingsPeriode.addAll(
            tilbakekrevingsperioder.map {
                DetaljertKravgrunnlagPeriodeDto().apply {
                    periode = PeriodeDto().apply {
                        this.fom = it.fom
                        this.tom = it.tom
                    }
                    tilbakekrevingsBelop.addAll(
                        emptyList(),
                    )
                    belopSkattMnd = BigDecimal.ZERO
                }
            },
        )
    }
}
