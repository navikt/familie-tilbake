package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.YearMonth

object KravgrunnlagValidator {
    private val log = TracedLogger.getLogger<KravgrunnlagValidator>()

    @Throws(UgyldigKravgrunnlagFeil::class)
    fun validerGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        log.medContext(logContext) {
            info("Validerer kravgrunnlag: ${kravgrunnlag.kravgrunnlagId}")
        }
        validerReferanse(kravgrunnlag)
        validerPeriodeInnenforMåned(kravgrunnlag)
        validerPeriodeStarterFørsteDagIMåned(kravgrunnlag)
        validerPeriodeSlutterSisteDagIMåned(kravgrunnlag)
        validerOverlappendePerioder(kravgrunnlag)
        validerSkatt(kravgrunnlag)
        validerPerioderHarFeilutbetalingspostering(kravgrunnlag)
        validerPerioderHarYtelsespostering(kravgrunnlag)
        validerPerioderHarFeilPosteringMedNegativFeilutbetaltBeløp(kravgrunnlag)
        validerYtelseMotFeilutbetaling(kravgrunnlag)
        validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt(kravgrunnlag)
    }

    private fun validerReferanse(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        log.medContext(logContext) {
            info("kravgrunnlag.referanse: ${kravgrunnlag.referanse }")
        }
        kravgrunnlag.referanse ?: throw UgyldigKravgrunnlagFeil(
            melding =
                "Ugyldig kravgrunnlag for kravgrunnlagId " +
                    "${kravgrunnlag.kravgrunnlagId}. Mangler referanse.",
            logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
        )
    }

    private fun validerPeriodeInnenforMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")

        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            val periode = it.periode
            val fomMåned = YearMonth.of(periode.fom.year, periode.fom.month)
            val tomMåned = YearMonth.of(periode.tom.year, periode.tom.month)
            if (fomMåned != tomMåned) {
                log.medContext(logContext) {
                    info("Ikke innenfor samme kalendermåned: $fomMåned og $tomMåned")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                            " Perioden ${periode.fom}-${periode.tom} er ikke innenfor en kalendermåned.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerPeriodeInnenforMåned: OK")
        }
    }

    private fun validerPeriodeStarterFørsteDagIMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.periode.fom.dayOfMonth != 1) {
                log.medContext(logContext) {
                    info("starter ikke første dag i måned: ${it.periode.fom}")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                            " Perioden ${it.periode.fom}-${it.periode.tom} starter ikke første dag i måned.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerPeriodeStarterFørsteDagIMåned: OK")
        }
    }

    private fun validerPeriodeSlutterSisteDagIMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.periode.tom.dayOfMonth != YearMonth.from(it.periode.tom).lengthOfMonth()) {
                log.medContext(logContext) {
                    info("slutter ikke siste dag i måned: ${it.periode.tom}")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                            " Perioden ${it.periode.fom}-${it.periode.tom} slutter ikke siste dag i måned.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerPeriodeSlutterSisteDagIMåned: OK")
        }
    }

    private fun validerPerioderHarFeilutbetalingspostering(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.tilbakekrevingsBelop.none { beløp -> finnesFeilutbetalingspostering(beløp.typeKlasse) }) {
                log.medContext(logContext) {
                    info("Perioden ${it.periode.fom}-${it.periode.tom} mangler postering med klassetype=FEIL")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                            "Perioden ${it.periode.fom}-${it.periode.tom} " +
                            "mangler postering med klassetype=FEIL.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerPerioderHarFeilutbetalingspostering: OK")
        }
    }

    private fun validerPerioderHarYtelsespostering(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.tilbakekrevingsBelop.none { beløp -> finnesYtelsespostering(beløp.typeKlasse) }) {
                log.medContext(logContext) {
                    info("Perioden ${it.periode.fom}-${it.periode.tom} mangler postering med klassetype=YTEL")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                            "Perioden ${it.periode.fom}-${it.periode.tom} " +
                            "mangler postering med klassetype=YTEL.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerPerioderHarYtelsespostering: OK")
        }
    }

    private fun validerOverlappendePerioder(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        val sortertePerioder: List<Månedsperiode> =
            kravgrunnlag.tilbakekrevingsPeriode
                .map { p -> Månedsperiode(p.periode.fom, p.periode.tom) }
                .sorted()
        for (i in 1 until sortertePerioder.size) {
            val forrigePeriode = sortertePerioder[i - 1]
            val nåværendePeriode = sortertePerioder[i]
            if (nåværendePeriode.fom <= forrigePeriode.tom) {
                log.medContext(logContext) {
                    info("Overlappende perioder $forrigePeriode og $nåværendePeriode")
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                            " Overlappende perioder $forrigePeriode og $nåværendePeriode.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerOverlappendePerioder: OK")
        }
    }

    private fun validerSkatt(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        val grupppertPåMåned: Map<YearMonth, List<DetaljertKravgrunnlagPeriodeDto>> =
            kravgrunnlag.tilbakekrevingsPeriode
                .groupBy { tilMåned(it.periode) }
                .toMap()

        for ((key, value) in grupppertPåMåned) {
            validerSkattForPeriode(key, value, kravgrunnlag.kravgrunnlagId, logContext)
        }
        log.medContext(logContext) {
            info("validerSkatt: OK")
        }
    }

    private fun validerSkattForPeriode(
        måned: YearMonth,
        perioder: List<DetaljertKravgrunnlagPeriodeDto>,
        kravgrunnlagId: BigInteger,
        logContext: SecureLog.Context,
    ) {
        var månedligSkattBeløp: BigDecimal? = null
        var totalSkatt = BigDecimal.ZERO
        for (periode in perioder) {
            if (månedligSkattBeløp == null) {
                månedligSkattBeløp = periode.belopSkattMnd
            } else {
                if (månedligSkattBeløp.compareTo(periode.belopSkattMnd) != 0) {
                    log.medContext(logContext) {
                        info("For måned $måned er opplyses ulike verdier maks skatt i ulike perioder")
                    }
                    throw UgyldigKravgrunnlagFeil(
                        melding =
                            "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                                "For måned $måned er opplyses ulike verdier maks skatt i ulike perioder",
                        logContext = logContext,
                    )
                }
            }
            for (postering in periode.tilbakekrevingsBelop) {
                totalSkatt += postering.belopTilbakekreves.multiply(postering.skattProsent)
            }
        }
        totalSkatt = totalSkatt.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
        if (månedligSkattBeløp == null) {
            log.medContext(logContext) {
                info("Mangler max skatt for måned $måned")
            }
            throw UgyldigKravgrunnlagFeil(
                melding =
                    "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                        "Mangler max skatt for måned $måned",
                logContext = logContext,
            )
        }
        if (totalSkatt > månedligSkattBeløp) {
            log.medContext(logContext) {
                info(
                    "For måned $måned er maks skatt $månedligSkattBeløp, " +
                        "men maks tilbakekreving ganget med skattesats blir $totalSkatt",
                )
            }
            throw UgyldigKravgrunnlagFeil(
                melding =
                    "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                        "For måned $måned er maks skatt $månedligSkattBeløp, " +
                        "men maks tilbakekreving ganget med skattesats blir $totalSkatt",
                logContext = logContext,
            )
        }
    }

    private fun validerPerioderHarFeilPosteringMedNegativFeilutbetaltBeløp(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            for (beløp in kravgrunnlagsperiode.tilbakekrevingsBelop) {
                if (finnesFeilutbetalingspostering(beløp.typeKlasse) && beløp.belopNy < BigDecimal.ZERO) {
                    log.medContext(logContext) {
                        info(
                            "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                                "Perioden ${kravgrunnlagsperiode.periode.fom}-${kravgrunnlagsperiode.periode.tom} " +
                                "har FEIL postering med negativ beløp",
                        )
                    }
                    throw UgyldigKravgrunnlagFeil(
                        melding =
                            "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                                "Perioden ${kravgrunnlagsperiode.periode.fom}-" +
                                "${kravgrunnlagsperiode.periode.tom} " +
                                "har FEIL postering med negativ beløp",
                        logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                    )
                }
            }
        }
        log.medContext(logContext) {
            info("validerPerioderHarFeilPosteringMedNegativFeilutbetaltBeløp: OK")
        }
    }

    private fun validerYtelseMotFeilutbetaling(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            val sumTilbakekrevesFraYtelsePosteringer =
                kravgrunnlagsperiode.tilbakekrevingsBelop
                    .filter { finnesYtelsespostering(it.typeKlasse) }
                    .sumOf(DetaljertKravgrunnlagBelopDto::getBelopTilbakekreves)
            val sumNyttBelopFraFeilposteringer =
                kravgrunnlagsperiode.tilbakekrevingsBelop
                    .filter { finnesFeilutbetalingspostering(it.typeKlasse) }
                    .sumOf(DetaljertKravgrunnlagBelopDto::getBelopNy)
            if (sumNyttBelopFraFeilposteringer.compareTo(sumTilbakekrevesFraYtelsePosteringer) != 0) {
                log.medContext(logContext) {
                    info(
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                            "For perioden ${kravgrunnlagsperiode.periode.fom}-${kravgrunnlagsperiode.periode.tom} " +
                            "total tilkakekrevesBeløp i YTEL posteringer er $sumTilbakekrevesFraYtelsePosteringer, " +
                            "mens total nytt beløp i FEIL posteringer er $sumNyttBelopFraFeilposteringer. " +
                            "Det er forventet at disse er like.",
                    )
                }
                throw UgyldigKravgrunnlagFeil(
                    melding =
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                            "For perioden ${kravgrunnlagsperiode.periode.fom}" +
                            "-${kravgrunnlagsperiode.periode.tom} total tilkakekrevesBeløp i YTEL " +
                            "posteringer er $sumTilbakekrevesFraYtelsePosteringer, mens total nytt beløp i " +
                            "FEIL posteringer er $sumNyttBelopFraFeilposteringer. " +
                            "Det er forventet at disse er like.",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
        log.medContext(logContext) {
            info("validerYtelseMotFeilutbetaling: OK")
        }
    }

    private fun validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val logContext = SecureLog.Context.utenBehandling("kravgullgaID: ${kravgrunnlag.kravgrunnlagId}")
        var harPeriodeMedBeløpMindreEnnDiff = false
        var harPeriodeMedBeløpStørreEnnDiff = false

        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            for (kgBeløp in kravgrunnlagsperiode.tilbakekrevingsBelop) {
                if (finnesYtelsespostering(kgBeløp.typeKlasse)) {
                    val diff: BigDecimal = kgBeløp.belopOpprUtbet.subtract(kgBeløp.belopNy)
                    if (kgBeløp.belopTilbakekreves > diff) {
                        harPeriodeMedBeløpStørreEnnDiff = true
                    } else {
                        harPeriodeMedBeløpMindreEnnDiff = true
                    }
                }
            }
        }

        // Hvis vi kun har YTEL-posteringer som er sørre enn diferansen mellom nyttBeløp og opprinneligBeløp
        // vil vi kaste en valideringsfeil
        if (harPeriodeMedBeløpStørreEnnDiff && !harPeriodeMedBeløpMindreEnnDiff) {
            log.medContext(logContext) {
                info(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                        "Har en eller flere perioder med YTEL-postering " +
                        "med tilbakekrevesBeløp som er større enn differansen mellom " +
                        "nyttBeløp og opprinneligBeløp",
                )
            }
            throw UgyldigKravgrunnlagFeil(
                melding =
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                        "Har en eller flere perioder med YTEL-postering " +
                        "med tilbakekrevesBeløp som er større enn differanse mellom " +
                        "nyttBeløp og opprinneligBeløp",
                logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
            )
        }

        log.medContext(logContext) {
            info("validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt: OK")
        }
    }

    private fun tilMåned(periode: PeriodeDto): YearMonth = YearMonth.of(periode.fom.year, periode.fom.month)

    private fun finnesFeilutbetalingspostering(typeKlasse: TypeKlasseDto): Boolean = Klassetype.FEIL.name == typeKlasse.value()

    private fun finnesYtelsespostering(typeKlasse: TypeKlasseDto): Boolean = Klassetype.YTEL.name == typeKlasse.value()
}
