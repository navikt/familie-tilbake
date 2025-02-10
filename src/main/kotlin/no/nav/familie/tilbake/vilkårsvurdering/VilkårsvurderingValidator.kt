package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningUtil
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
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
        if (aktsomhetDto?.særligeGrunner != null) {
            val særligGrunner = aktsomhetDto.særligeGrunner
            when {
                særligGrunner.any { SærligGrunn.ANNET != it.særligGrunn && it.begrunnelse != null } -> {
                    throw Feil(
                        message = "Begrunnelse kan fylles ut kun for ANNET begrunnelse",
                        frontendFeilmelding = "Begrunnelse kan fylles ut kun for ANNET begrunnelse",
                        logContext = logContext,
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
                særligGrunner.any { SærligGrunn.ANNET == it.særligGrunn && it.begrunnelse == null } -> {
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
        val feilMelding = "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
        if (vilkårsvurderingsperiode.godTroDto?.beløpTilbakekreves != null) {
            val feilutbetalteBeløp = KravgrunnlagsberegningUtil.beregnFeilutbetaltBeløp(kravgrunnlag431, periode)
            if (vilkårsvurderingsperiode.godTroDto.beløpTilbakekreves > feilutbetalteBeløp) {
                throw Feil(
                    message = feilMelding,
                    frontendFeilmelding = feilMelding,
                    logContext = logContext,
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
        if (vilkårsvurderingsperiode.aktsomhetDto?.beløpTilbakekreves != null) {
            val feilutbetalteBeløp = KravgrunnlagsberegningUtil.beregnFeilutbetaltBeløp(kravgrunnlag431, periode)
            if (vilkårsvurderingsperiode.aktsomhetDto.beløpTilbakekreves > feilutbetalteBeløp) {
                throw Feil(
                    message = feilMelding,
                    frontendFeilmelding = feilMelding,
                    logContext = logContext,
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
    }
}
