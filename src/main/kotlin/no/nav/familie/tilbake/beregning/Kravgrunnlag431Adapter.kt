package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class Kravgrunnlag431Adapter(private val kravgrunnlag431: Kravgrunnlag431) : KravgrunnlagAdapter {
    override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
        return kravgrunnlag431.perioder.map(::PeriodeAdapter)
    }

    fun feilutbetaltBeløp(periode: Datoperiode) = perioder()
        .filter { it.periode() in periode }
        .sumOf(KravgrunnlagPeriodeAdapter::feilutbetaltYtelsesbeløp)
        .setScale(0, RoundingMode.HALF_UP)

    class PeriodeAdapter(private val periode: Kravgrunnlagsperiode432) : KravgrunnlagPeriodeAdapter {
        override fun periode(): Datoperiode {
            return periode.periode.toDatoperiode()
        }

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return periode.beløp
                .filter { it.klassetype == Klassetype.FEIL }
                .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return periode.beløp.filter { it.klassetype == Klassetype.YTEL }.map(::KravgrunnlagBeløpAdapter)
        }

        class KravgrunnlagBeløpAdapter(val kravgrunnlagBeløp: Kravgrunnlagsbeløp433) : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
            override fun klassekode(): String = kravgrunnlagBeløp.klassekode.tilKlassekodeNavn()

            override fun tilbakekrevesBeløp(): BigDecimal = kravgrunnlagBeløp.tilbakekrevesBeløp.setScale(0, RoundingMode.HALF_DOWN)

            override fun utbetaltYtelsesbeløp(): BigDecimal = kravgrunnlagBeløp.opprinneligUtbetalingsbeløp.setScale(0, RoundingMode.HALF_DOWN)

            override fun riktigYteslesbeløp(): BigDecimal = kravgrunnlagBeløp.nyttBeløp.setScale(0, RoundingMode.HALF_DOWN)

            override fun skatteprosent(): BigDecimal = kravgrunnlagBeløp.skatteprosent
        }
    }
}
