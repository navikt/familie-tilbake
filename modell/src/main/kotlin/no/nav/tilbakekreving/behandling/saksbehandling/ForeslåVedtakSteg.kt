package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

class ForeslåVedtakSteg(
    private val faktasteg: Faktasteg,
    private val foreldelsesteg: Foreldelsesteg,
    private val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
) : Saksbehandlingsteg<BeregningsresultatDto> {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstending(): Boolean = false

    private fun lagBeregning(): Beregning {
        return Beregning(
            beregnRenter = false,
            tilbakekrevLavtBeløp = true,
            vilkårsvurderingsteg,
            foreldelsesteg.perioder(),
            kravgrunnlag.entry,
        )
    }

    override fun tilFrontendDto(): BeregningsresultatDto {
        val beregning = lagBeregning().beregn()
        return BeregningsresultatDto(
            beregning.beregningsresultatsperioder.map {
                BeregningsresultatsperiodeDto(
                    periode = it.periode,
                    vurdering = it.vurdering,
                    feilutbetaltBeløp = it.feilutbetaltBeløp,
                    andelAvBeløp = it.andelAvBeløp,
                    renteprosent = it.renteprosent,
                    tilbakekrevingsbeløp = it.tilbakekrevingsbeløp,
                    tilbakekrevesBeløpEtterSkatt = it.tilbakekrevingsbeløpEtterSkatt,
                )
            },
            beregning.vedtaksresultat,
            faktasteg.tilFrontendDto().vurderingAvBrukersUttalelse,
        )
    }
}
