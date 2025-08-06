package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.entities.BeløpEntity
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.entities.KravgrunnlagPeriodeEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
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
    internal val fagsystemVedtaksdato: LocalDate?,
    val vedtakGjelder: Aktør,
    private val utbetalesTil: Aktør,
    private val skalBeregneRenter: Boolean,
    private val ansvarligEnhet: String,
    private val kontrollfelt: String,
    internal val kravgrunnlagId: String,
    // Brukes som eksternId i henting av fagsysteminfo, hva betyr det egentlig?
    val referanse: String,
    val perioder: List<Periode>,
    sporing: Sporing,
) : Historikk.HistorikkInnslag<UUID>, KravgrunnlagAdapter {
    init {
        if (vedtakGjelder !is Aktør.Person || utbetalesTil !is Aktør.Person) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagIkkePerson, sporing)
        }

        if (vedtakGjelder.ident != utbetalesTil.ident) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBrukerIkkeLikMottaker, sporing)
        }
    }

    fun totaltBeløpFor(periode: Datoperiode): BigDecimal =
        perioder.single { kgPeriode -> kgPeriode.inneholder(periode) }
            .feilutbetaltYtelsesbeløp()

    fun datoperioder() = perioder.map { it.periode }

    fun feilutbetaltBeløpForAllePerioder() = perioder.sumOf { it.feilutbetaltYtelsesbeløp() }

    fun totaltFeilutbetaltPeriode() = datoperioder().minOf { it.fom } til datoperioder().maxOf { it.tom }

    override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
        return perioder
    }

    fun tilEntity(sporing: Sporing): KravgrunnlagHendelseEntity {
        return KravgrunnlagHendelseEntity(
            internId = internId,
            vedtakId = vedtakId,
            kravstatuskode = kravstatuskode,
            fagsystemVedtaksdato = fagsystemVedtaksdato,
            vedtakGjelder = vedtakGjelder.tilEntity(),
            utbetalesTil = utbetalesTil.tilEntity(),
            skalBeregneRenter = skalBeregneRenter,
            ansvarligEnhet = ansvarligEnhet,
            kontrollfelt = kontrollfelt,
            kravgrunnlagId = kravgrunnlagId,
            referanse = referanse,
            perioder = perioder.map { it.tilEntity() },
            sporing = sporing,
        )
    }

    class Periode(
        val periode: Datoperiode,
        private val månedligSkattebeløp: BigDecimal,
        private val ytelsesbeløp: List<Beløp>,
        private val feilutbetaltBeløp: List<Beløp>,
    ) : KravgrunnlagPeriodeAdapter {
        fun inneholder(other: Datoperiode): Boolean = periode.inneholder(other)

        override fun periode(): Datoperiode {
            return periode
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return ytelsesbeløp + feilutbetaltBeløp
        }

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return ytelsesbeløp.sumOf { it.tilbakekrevesBeløp }
        }

        fun tilEntity(): KravgrunnlagPeriodeEntity {
            return KravgrunnlagPeriodeEntity(
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                månedligSkattebeløp = månedligSkattebeløp,
                ytelsesbeløp = ytelsesbeløp.map { it.tilEntity() },
                feilutbetaltBeløp = feilutbetaltBeløp.map { it.tilEntity() },
            )
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

            fun tilEntity(): BeløpEntity {
                return BeløpEntity(
                    klassekode = klassekode,
                    klassetype = klassetype,
                    opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
                    nyttBeløp = nyttBeløp,
                    tilbakekrevesBeløp = tilbakekrevesBeløp,
                    skatteprosent = skatteprosent,
                )
            }
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
        NY("Nytt kravgrunnlag"),
        SPERRET("Kravgrunnlag sperret"),
    }
}
