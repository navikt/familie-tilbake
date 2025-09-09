package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Friteksttype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.tilbakekreving.api.v1.dto.PeriodeMedTekstDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnTyper
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype.ORDINÆR
import org.springframework.http.HttpStatus

object VedtaksbrevFritekstValidator {
    @Throws(Feil::class)
    fun validerObligatoriskeFritekster(
        behandling: Behandling,
        faktaFeilutbetaling: FaktaFeilutbetaling,
        vilkårsvurdering: Vilkårsvurdering?,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        vedtaksbrevstype: Vedtaksbrevstype,
        gjelderDødsfall: Boolean,
        logContext: SecureLog.Context,
    ) {
        validerFritekster(behandling, faktaFeilutbetaling, avsnittMedPerioder, vedtaksbrevsoppsummering, vedtaksbrevstype, logContext)

        val revurderingHeltEllerDelvisBortfalt = behandling.årsaker.any {
            it.type == Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT
        }
        if (vilkårsvurdering != null && !gjelderDødsfall && !revurderingHeltEllerDelvisBortfalt) {
            validerFritekstISærligGrunnerAnnetAvsnitt(
                vilkårsvurdering,
                vedtaksbrevFritekstPerioder,
                vedtaksbrevsoppsummering.skalSammenslåPerioder == SkalSammenslåPerioder.JA,
                logContext,
            )
        }

        if (ORDINÆR == vedtaksbrevstype) {
            validerFritekstIFaktaAvsnitt(
                faktaFeilutbetaling,
                vedtaksbrevFritekstPerioder,
                avsnittMedPerioder,
                vedtaksbrevsoppsummering.skalSammenslåPerioder == SkalSammenslåPerioder.JA,
                logContext,
            )
        }
        validerNårOppsummeringsfritekstErPåkrevd(behandling, vedtaksbrevsoppsummering, logContext)
    }

    fun validerFritekster(
        behandling: Behandling,
        faktaFeilutbetaling: FaktaFeilutbetaling,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        vedtaksbrevstype: Vedtaksbrevstype,
        logContext: SecureLog.Context,
    ) {
        validerPerioder(behandling, avsnittMedPerioder, faktaFeilutbetaling, logContext)
        validerOppsummeringsfritekstLengde(behandling, vedtaksbrevsoppsummering, vedtaksbrevstype, logContext)
    }

    private fun validerPerioder(
        behandling: Behandling,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        faktaFeilutbetaling: FaktaFeilutbetaling,
        logContext: SecureLog.Context,
    ) {
        avsnittMedPerioder.forEach {
            if (!faktaFeilutbetaling.perioder.any { faktaPeriode ->
                    faktaPeriode.periode.inneholder(it.periode.toMånedsperiode())
                }
            ) {
                throw Feil(
                    message = "Periode ${it.periode.fom}-${it.periode.tom} er ugyldig for behandling ${behandling.id}",
                    frontendFeilmelding =
                        "Periode ${it.periode.fom}-${it.periode.tom} er ugyldig " +
                            "for behandling ${behandling.id}",
                    logContext = logContext,
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
    }

    private fun validerNårOppsummeringsfritekstErPåkrevd(
        behandling: Behandling,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        logContext: SecureLog.Context,
    ) {
        val revurderingIkkeOpprettetEtterKlage =
            behandling.årsaker.none {
                it.type in
                    setOf(
                        Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                        Behandlingsårsakstype.REVURDERING_KLAGE_NFP,
                    )
            }
        if (Behandlingstype.REVURDERING_TILBAKEKREVING == behandling.type &&
            revurderingIkkeOpprettetEtterKlage &&
            vedtaksbrevsoppsummering.oppsummeringFritekst.isNullOrEmpty()
        ) {
            throw Feil(
                message = "oppsummering fritekst påkrevet for revurdering ${behandling.id}",
                frontendFeilmelding = "oppsummering fritekst påkrevet for revurdering ${behandling.id}",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerOppsummeringsfritekstLengde(
        behandling: Behandling,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        vedtaksbrevstype: Vedtaksbrevstype,
        logContext: SecureLog.Context,
    ) {
        val maksTekstLengde =
            when (vedtaksbrevstype) {
                ORDINÆR -> 4000
                else -> 10000
            }
        if (vedtaksbrevsoppsummering.oppsummeringFritekst != null &&
            vedtaksbrevsoppsummering.oppsummeringFritekst.length > maksTekstLengde
        ) {
            throw Feil(
                message = "Oppsummeringstekst er for lang for behandling ${behandling.id}",
                frontendFeilmelding = "Oppsummeringstekst er for lang for behandling ${behandling.id}",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerFritekstIFaktaAvsnitt(
        faktaFeilutbetaling: FaktaFeilutbetaling,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        skalSammenslåPerioder: Boolean,
        logContext: SecureLog.Context,
    ) {
        val faktaPerioderMedFritekst = faktaFeilutbetaling.perioder.filter { Hendelsesundertype.ANNET_FRITEKST == it.hendelsesundertype }.sortedBy { it.periode }
        val validerPerioder = if (skalSammenslåPerioder && faktaPerioderMedFritekst.isNotEmpty()) listOf(faktaPerioderMedFritekst.first()) else faktaPerioderMedFritekst

        validerPerioder
            .forEach { faktaFeilutbetalingsperiode ->
                val perioder =
                    finnFritekstPerioder(
                        vedtaksbrevFritekstPerioder,
                        faktaFeilutbetalingsperiode.periode,
                        Friteksttype.FAKTA,
                    )
                if (perioder.isEmpty()) {
                    throw Feil(
                        message = "Mangler fakta fritekst for alle fakta perioder",
                        frontendFeilmelding = "Mangler Fakta fritekst for alle fakta perioder",
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
                // Hvis en av de periodene mangler fritekst
                val omsluttetPerioder =
                    avsnittMedPerioder.filter {
                        faktaFeilutbetalingsperiode.periode.inneholder(it.periode.toMånedsperiode())
                    }
                omsluttetPerioder.forEach {
                    if (it.faktaAvsnitt.isNullOrBlank()) {
                        throw Feil(
                            message = "Mangler fakta fritekst for ${it.periode.fom}-${it.periode.tom}",
                            frontendFeilmelding = "Mangler Fakta fritekst for ${it.periode.fom}-${it.periode.tom}",
                            logContext = logContext,
                            httpStatus = HttpStatus.BAD_REQUEST,
                        )
                    }
                }
            }
    }

    private fun validerFritekstISærligGrunnerAnnetAvsnitt(
        vilkårsvurdering: Vilkårsvurdering,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        skalSammenslåPerioder: Boolean,
        logContext: SecureLog.Context,
    ) {
        val perioderMedSærligeGrunner =
            vilkårsvurdering.perioder
                .filter {
                    it.aktsomhet?.vilkårsvurderingSærligeGrunner != null &&
                        it.aktsomhet.vilkårsvurderingSærligeGrunner.any { særligGrunn -> SærligGrunnTyper.ANNET == særligGrunn.særligGrunn }
                }.sortedBy { it.periode }

        val validerPerioder =
            if (skalSammenslåPerioder && perioderMedSærligeGrunner.isNotEmpty()) {
                setOf(perioderMedSærligeGrunner.first())
            } else {
                perioderMedSærligeGrunner
            }

        validerPerioder
            .forEach {
                val perioder =
                    finnFritekstPerioder(
                        vedtaksbrevFritekstPerioder,
                        it.periode,
                        Friteksttype.SÆRLIGE_GRUNNER_ANNET,
                    )

                if (perioder.isEmpty()) {
                    SecureLog.medContext(logContext) {
                        warn(
                            "Fant ikke fritekst for ANNET særlige grunner for vedtaksbrev. periode: {}, tilgjengelige fritekstfelter: {}",
                            it.periode.toString(),
                            vedtaksbrevFritekstPerioder.joinToString(",") { fritekst -> "periode=${fritekst.periode}, type=${fritekst.fritekststype}" },
                        )
                    }
                    throw Feil(
                        message = "Mangler ANNET Særliggrunner fritekst for ${it.periode}",
                        frontendFeilmelding = "Mangler ANNET Særliggrunner fritekst for ${it.periode} ",
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
            }
    }

    private fun finnFritekstPerioder(
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        vurdertPeriode: Månedsperiode,
        friteksttype: Friteksttype,
    ): List<Vedtaksbrevsperiode> =
        vedtaksbrevFritekstPerioder.filter {
            friteksttype == it.fritekststype &&
                vurdertPeriode.inneholder(it.periode)
        }
}
