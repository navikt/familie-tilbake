package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class Kravgrunnlag431Adapter(private val kravgrunnlag431: Kravgrunnlag431) : KravgrunnlagAdapter {
    override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
        return kravgrunnlag431.perioder.map(::PeriodeAdapter)
    }

    class PeriodeAdapter(private val periode: Kravgrunnlagsperiode432) : KravgrunnlagPeriodeAdapter {
        override fun periode(): Datoperiode {
            return periode.periode.toDatoperiode()
        }

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return periode.beløp
                .filter { it.klassetype == Klassetype.FEIL }
                .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
        }

        override fun utbetaltYtelsesbeløp(): BigDecimal {
            return periode.beløp
                .filter { it.klassetype == Klassetype.YTEL }
                .sumOf(Kravgrunnlagsbeløp433::opprinneligUtbetalingsbeløp)
        }

        override fun riktigYteslesbeløp(): BigDecimal {
            return periode.beløp
                .filter { it.klassetype == Klassetype.YTEL }
                .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return periode.beløp.map(::KravgrunnlagBeløpAdapter)
        }

        class KravgrunnlagBeløpAdapter(val kravgrunnlagBeløp: Kravgrunnlagsbeløp433) : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
            override fun beløp(): BigDecimal {
                return kravgrunnlagBeløp.tilbakekrevesBeløp
            }

            override fun skatteprosent(): BigDecimal {
                return kravgrunnlagBeløp.skatteprosent
            }
        }
    }
}
