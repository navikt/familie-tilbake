package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
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
    val hjemlerForTilbakekreving: List<HjemmelForTilbakekreving>,
    val beregnerSkatt: Boolean,
    val behandlingstype: Behandlingstype,
)

data class InfobrevData(
    val brukerdata: BrevmottakerDto,
    val tittel: String,
    val innhold: List<String>,
    val underavsnitt: List<Infobrevavsnitt>,
    val enhetNavn: String,
    val sendtDato: LocalDate,
    val tilbakekrevingId: String,
)

data class Infobrevavsnitt(
    val tittel: String,
    val avsnitt: List<String>,
)
