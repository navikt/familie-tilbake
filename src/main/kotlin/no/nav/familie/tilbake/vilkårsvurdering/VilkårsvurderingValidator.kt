package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.beregning.Kravgrunnlag431Adapter
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnTyper
import org.springframework.http.HttpStatus
import java.math.BigDecimal

object VilkårsvurderingValidator {
    @Throws(Feil::class)
    fun validerVilkårsvurdering(
        vilkårsvurderingDto: BehandlingsstegVilkårsvurderingDto,
        kravgrunnlag431: Kravgrunnlag431,
        logContext: SecureLog.Context,
    ) {
        vilkårsvurderingDto.vilkårsvurderingsperioder.forEach {
            validerAndelTilbakekrevesBeløp(it.aktsomhetDto, logContext)
            validerAnnetBegrunnelse(it.aktsomhetDto, logContext)
            validerBeløp(kravgrunnlag431, Månedsperiode(it.periode.fom, it.periode.tom), it, logContext)
        }
    }

    private fun validerAndelTilbakekrevesBeløp(
        aktsomhetDto: AktsomhetDto?,
        logContext: SecureLog.Context,
    ) {
        if (aktsomhetDto?.andelTilbakekreves?.compareTo(BigDecimal(100)) == 1) {
            throw Feil(
                message = "Andel som skal tilbakekreves kan ikke være mer enn 100 prosent",
                frontendFeilmelding = "Andel som skal tilbakekreves kan ikke være mer enn 100 prosent",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerAnnetBegrunnelse(
        aktsomhetDto: AktsomhetDto?,
        logContext: SecureLog.Context,
    ) {
        val særligeGrunner = aktsomhetDto?.særligeGrunner
        if (særligeGrunner != null) {
            when {
                særligeGrunner.any { SærligGrunnTyper.ANNET != it.særligGrunn && it.begrunnelse != null } -> {
                    throw Feil(
                        message = "Begrunnelse kan fylles ut kun for ANNET begrunnelse",
                        frontendFeilmelding = "Begrunnelse kan fylles ut kun for ANNET begrunnelse",
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
                særligeGrunner.any { SærligGrunnTyper.ANNET == it.særligGrunn && it.begrunnelse == null } -> {
                    throw Feil(
                        message = "ANNET særlig grunner må ha ANNET begrunnelse",
                        frontendFeilmelding = "ANNET særlig grunner må ha ANNET begrunnelse",
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
            }
        }
    }

    private fun validerBeløp(
        kravgrunnlag431: Kravgrunnlag431,
        periode: Månedsperiode,
        vilkårsvurderingsperiode: VilkårsvurderingsperiodeDto,
        logContext: SecureLog.Context,
    ) {
        validerTilbakrevetBeløp(vilkårsvurderingsperiode.godTroDto?.beløpTilbakekreves, kravgrunnlag431, periode, logContext)
        validerTilbakrevetBeløp(vilkårsvurderingsperiode.aktsomhetDto?.beløpTilbakekreves, kravgrunnlag431, periode, logContext)
    }

    fun validerTilbakrevetBeløp(
        beløp: BigDecimal?,
        kravgrunnlag431: Kravgrunnlag431,
        periode: Månedsperiode,
        logContext: SecureLog.Context,
    ) {
        if (beløp == null) return

        val feilutbetalteBeløp = Kravgrunnlag431Adapter(kravgrunnlag431).feilutbetaltBeløp(periode.toDatoperiode())
        val feilmelding = "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
        if (beløp > feilutbetalteBeløp) {
            throw Feil(
                message = feilmelding,
                frontendFeilmelding = feilmelding,
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
