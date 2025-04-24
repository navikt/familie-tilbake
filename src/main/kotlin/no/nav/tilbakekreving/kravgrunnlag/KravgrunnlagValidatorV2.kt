package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.beregning.TilbakekrevingsberegningVilkår
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import java.math.BigDecimal
import java.math.RoundingMode

class KravgrunnlagValidatorV2(
    private val kravgrunnlag: DetaljertKravgrunnlagDto,
    private val periodeValidator: PeriodeValidator,
) {
    private val feil = mutableListOf<ValidationResult.Feil.Failure>()

    fun valider(): ValidationResult {
        validerReferanse()
        validerPerioder()
        validerSkatt()
        validerPerioderHarFeilutbetalingspostering()
        validerPerioderHarYtelsespostering()
        validerFeilposteringMedNegativtBeløp()
        validerYtelseMotFeilutbetaling()
        validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt()
        return when {
            feil.isEmpty() -> ValidationResult.Ok
            else -> ValidationResult.Feil(kravgrunnlag.kravgrunnlagId.toString(), feil)
        }
    }

    private fun validerReferanse() {
        failOn(kravgrunnlag.referanse == null) { "Mangler referanse" }
    }

    private fun validerPerioder() {
        val perioder = kravgrunnlag.tilbakekrevingsPeriode
            .map { it.periode.fom til it.periode.tom }
            .sortedBy { it.fom }

        perioder.forEach { feil.addAll(periodeValidator.valider(it)) }
        perioder
            .zipWithNext()
            .forEach { (nåværende, neste) ->
                failOn(neste.fom <= nåværende.tom) {
                    "Perioden ${nåværende.fom} til ${nåværende.tom} overlapper med perioden ${neste.fom} til ${neste.tom}"
                }
            }
    }

    private fun validerSkatt() {
        kravgrunnlag.tilbakekrevingsPeriode.forEach { periode ->
            validerSkattForPeriode(periode)
        }
    }

    private fun validerSkattForPeriode(
        periode: DetaljertKravgrunnlagPeriodeDto,
    ) {
        val totalSkatt = periode.tilbakekrevingsBelop
            .sumOf { it.belopTilbakekreves.multiply(it.skattProsent) }
            .divide(TilbakekrevingsberegningVilkår.HUNDRE_PROSENT, 0, RoundingMode.DOWN)

        failOn(totalSkatt > periode.belopSkattMnd) {
            "Maks skatt for perioden ${periode.periode.fom} til ${periode.periode.tom} er ${periode.belopSkattMnd}, " +
                "men maks tilbakekreving ganget med skattesats blir $totalSkatt"
        }
    }

    private fun validerPerioderHarFeilutbetalingspostering() {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            failOn(it.tilbakekrevingsBelop.none { beløp -> beløp.typeKlasse == TypeKlasseDto.FEIL }) {
                "Perioden ${it.periode.fom} til ${it.periode.tom} mangler postering med klassetype=FEIL"
            }
        }
    }

    private fun validerPerioderHarYtelsespostering() {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            failOn(it.tilbakekrevingsBelop.none { beløp -> beløp.typeKlasse == TypeKlasseDto.YTEL }) {
                "Perioden ${it.periode.fom} til ${it.periode.tom} mangler postering med klassetype=YTEL"
            }
        }
    }

    private fun validerFeilposteringMedNegativtBeløp() {
        kravgrunnlag.tilbakekrevingsPeriode.forEach { periode ->
            periode.tilbakekrevingsBelop
                .filter { it.typeKlasse == TypeKlasseDto.FEIL }
                .forEach {
                    failOn(it.belopNy < BigDecimal.ZERO) {
                        "Perioden ${periode.periode.fom} til ${periode.periode.tom} har feilpostering med negativt beløp"
                    }
                }
        }
    }

    private fun validerYtelseMotFeilutbetaling() {
        kravgrunnlag.tilbakekrevingsPeriode.forEach { kravgrunnlagsperiode ->
            val sumTilbakekrevesFraYtelsePosteringer =
                kravgrunnlagsperiode.tilbakekrevingsBelop
                    .filter { it.typeKlasse == TypeKlasseDto.YTEL }
                    .sumOf(DetaljertKravgrunnlagBelopDto::getBelopTilbakekreves)
                    .setScale(2)
            val sumNyttBelopFraFeilposteringer =
                kravgrunnlagsperiode.tilbakekrevingsBelop
                    .filter { it.typeKlasse == TypeKlasseDto.FEIL }
                    .sumOf(DetaljertKravgrunnlagBelopDto::getBelopNy)
                    .setScale(2)

            failOn(sumNyttBelopFraFeilposteringer.compareTo(sumTilbakekrevesFraYtelsePosteringer) != 0) {
                "Perioden ${kravgrunnlagsperiode.periode.fom} til ${kravgrunnlagsperiode.periode.tom} har ulikt summert tilbakekrevingsbeløp i YTEL postering($sumTilbakekrevesFraYtelsePosteringer) " +
                    "i forhold til summert beløpNy i FEIL postering($sumNyttBelopFraFeilposteringer)"
            }
        }
    }

    private fun validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt() {
        val (beløpStørreEnnDiff, beløpMindreEnnDiff) = kravgrunnlag.tilbakekrevingsPeriode
            .flatMap { it.tilbakekrevingsBelop }
            .filter { it.typeKlasse == TypeKlasseDto.YTEL }
            .partition { kgBeløp ->
                val diff = kgBeløp.belopOpprUtbet - kgBeløp.belopNy
                kgBeløp.belopTilbakekreves > diff
            }

        // Hvis vi kun har YTEL-posteringer som er større enn diferansen mellom nyttBeløp og opprinneligBeløp
        failOn(beløpStørreEnnDiff.isNotEmpty() && beløpMindreEnnDiff.isEmpty()) {
            "Har en eller flere perioder med YTEL-postering med tilbakekrevesBeløp som er større enn differanse mellom nyttBeløp og opprinneligBeløp"
        }
    }

    private fun failOn(
        failureCondition: Boolean,
        message: () -> String,
    ) {
        if (failureCondition) {
            feil.add(ValidationResult.Feil.Failure(message()))
        }
    }

    companion object {
        fun valider(
            kravgrunnlag: DetaljertKravgrunnlagDto,
            periodeValidator: PeriodeValidator,
        ): ValidationResult {
            return KravgrunnlagValidatorV2(kravgrunnlag, periodeValidator)
                .valider()
        }
    }
}
