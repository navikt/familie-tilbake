package no.nav.tilbakekreving.hendelse

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
) : Historikk.HistorikkInnslag<UUID> {
    fun totaltBeløpFor(periode: Datoperiode): BigDecimal =
        perioder.single { kgPeriode -> kgPeriode.inneholder(periode) }
            .totaltBeløp()

    fun datoperioder() = perioder.map { it.periode }

    fun feilutbetaltBeløpForAllePerioder() = perioder.sumOf { it.totaltBeløp() }

    fun totaltFeilutbetaltPeriode() = datoperioder().minOf { it.fom } til datoperioder().maxOf { it.tom }

    class Periode(
        val periode: Datoperiode,
        private val månedligSkattebeløp: BigDecimal,
        private val beløp: List<Beløp>,
    ) {
        fun inneholder(other: Datoperiode): Boolean = periode.inneholder(other)

        fun totaltBeløp() = beløp.sumOf { it.tilbakekrevesBeløp }

        data class Beløp(
            private val klassekode: String,
            private val klassetype: String,
            private val opprinneligUtbetalingsbeløp: BigDecimal,
            private val nyttBeløp: BigDecimal,
            val tilbakekrevesBeløp: BigDecimal,
            private val skatteprosent: BigDecimal,
        )
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
