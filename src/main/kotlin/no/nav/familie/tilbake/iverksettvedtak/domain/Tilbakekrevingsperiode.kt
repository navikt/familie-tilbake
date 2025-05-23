package no.nav.familie.tilbake.iverksettvedtak.domain

import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import java.math.BigDecimal

// Brukes bare for iverksettelse

data class Tilbakekrevingsperiode(
    val periode: Månedsperiode,
    val renter: BigDecimal,
    val beløp: List<Tilbakekrevingsbeløp>,
)

data class Tilbakekrevingsbeløp(
    val klassetype: Klassetype,
    val klassekode: Klassekode,
    val nyttBeløp: BigDecimal,
    val utbetaltBeløp: BigDecimal,
    val tilbakekrevesBeløp: BigDecimal,
    val uinnkrevdBeløp: BigDecimal,
    val skattBeløp: BigDecimal,
    val kodeResultat: KodeResultat,
)

enum class KodeResultat(
    val kode: String,
) {
    FORELDET("FORELDET"),
    FEILREGISTRERT("FEILREGISTRERT"),
    INGEN_TILBAKEKREVING("INGEN_TILBAKEKREV"),
    DELVIS_TILBAKEKREVING("DELVIS_TILBAKEKREV"),
    FULL_TILBAKEKREVING("FULL_TILBAKEKREV"),
}
