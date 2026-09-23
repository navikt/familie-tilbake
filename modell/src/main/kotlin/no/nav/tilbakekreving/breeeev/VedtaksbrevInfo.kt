package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.YtelseDto
import java.time.LocalDate

data class VedtaksbrevInfo(
    val brukerdata: BrevmottakerDto,
    val skalTilbakekreves: Boolean,
    val ytelse: YtelseDto,
    val signatur: Signatur,
    val perioder: List<BegrunnetPeriode>,
    val bunntekster: Set<Bunntekst>,
    val tilbakekrevingId: String,
    val beregningsresultat: List<BeregningsresultatsperiodeDto>,
    val vedtaksbrevOppsummeringstabell: List<VedtaksbrevOppsummeringstabell>,
    val hjemlerForTilbakekreving: List<HjemmelForTilbakekreving>,
    val beregnerSkatt: Boolean,
)

data class VedtaksbrevOppsummeringstabell(
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int,
    val beløpIbehold: Int?,
    val redusertBeløp: Int,
    val rentebeløp: Int,
    val skattebeløp: Int,
    val tilbakekrevingsbeløp: Int,
)
