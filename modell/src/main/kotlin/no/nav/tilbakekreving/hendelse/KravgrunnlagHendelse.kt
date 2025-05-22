package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

class KravgrunnlagHendelse(
    override val internId: UUID,
    private val vedtakId: BigInteger,
    private val kravstatuskode: Kravstatuskode,
    private val fagsystemVedtaksdato: LocalDate,
    private val vedtakGjelder: Aktør,
    private val utbetalesTil: Aktør,
    private val skalBeregneRenter: Boolean,
    private val ansvarligEnhet: String,
    private val kontrollfelt: String,
    private val kravgrunnlagId: String,
    // Brukes som eksternId i henting av fagsysteminfo, hva betyr det egentlig?
    val referanse: String,
    val perioder: List<Periode>,
) : Historikk.HistorikkInnslag<UUID>, KravgrunnlagAdapter {
    fun totaltBeløpFor(periode: Datoperiode): BigDecimal =
        perioder.single { kgPeriode -> kgPeriode.inneholder(periode) }
            .totaltBeløp()

    fun datoperioder() = perioder.map { it.periode }

    fun feilutbetaltBeløpForAllePerioder() = perioder.sumOf { it.totaltBeløp() }

    fun totaltFeilutbetaltPeriode() = datoperioder().minOf { it.fom } til datoperioder().maxOf { it.tom }

    override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
        return perioder
    }

    class Periode(
        val periode: Datoperiode,
        private val månedligSkattebeløp: BigDecimal,
        private val ytelsesbeløp: List<Beløp>,
        private val feilutbetaltBeløp: List<Beløp>,
    ) : KravgrunnlagPeriodeAdapter {
        fun inneholder(other: Datoperiode): Boolean = periode.inneholder(other)

        fun totaltBeløp() = feilutbetaltBeløp.sumOf { it.tilbakekrevesBeløp }

        override fun periode(): Datoperiode {
            return periode
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return ytelsesbeløp + feilutbetaltBeløp
        }

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return totaltBeløp()
        }

        data class Beløp(
            private val klassekode: String,
            private val klassetype: String,
            val opprinneligUtbetalingsbeløp: BigDecimal,
            val nyttBeløp: BigDecimal,
            val tilbakekrevesBeløp: BigDecimal,
            private val skatteprosent: BigDecimal,
        ) : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
            override fun klassekode() = klassekode

            override fun tilbakekrevesBeløp(): BigDecimal = tilbakekrevesBeløp

            override fun skatteprosent(): BigDecimal = skatteprosent

            override fun utbetaltYtelsesbeløp(): BigDecimal = opprinneligUtbetalingsbeløp

            override fun riktigYteslesbeløp(): BigDecimal = nyttBeløp
        }
    }

    enum class Kravstatuskode(
        val navn: String,
    ) {
        ANNULERT("Kravgrunnlag annullert"),
        ANNULLERT_OMG("Kravgrunnlag annullert ved omg"),
        AVSLUTTET("Avsluttet kravgrunnlag"),
        BEHANDLET("Kravgrunnlag ferdigbehandlet"),
        ENDRET("Endret kravgrunnlag"),
        FEIL("Feil på kravgrunnlag"),
        MANUELL("Manuell behandling"),
        NYTT("Nytt kravgrunnlag"),
        SPERRET("Kravgrunnlag sperret"),
    }

    sealed interface Aktør {
        val ident: String

        data class Person(override val ident: String) : Aktør

        data class Organisasjon(override val ident: String) : Aktør

        data class Samhandler(override val ident: String) : Aktør

        data class Applikasjonsbruker(override val ident: String) : Aktør
    }
}
