package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.BeløpEntity
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.entities.KravgrunnlagPeriodeEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

class KravgrunnlagHendelse(
    override val id: UUID,
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
    private val perioder: List<Periode>,
) : Historikk.HistorikkInnslag<UUID>, KravgrunnlagAdapter {
    fun valider(sporing: Sporing) {
        if (vedtakGjelder !is Aktør.Person || utbetalesTil !is Aktør.Person) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagIkkePerson, sporing)
        }

        if (vedtakGjelder.ident != utbetalesTil.ident) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBrukerIkkeLikMottaker, sporing)
        }

        if (kravstatuskode != Kravstatuskode.NY) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttet, sporing)
        }
    }

    fun totaltBeløpFor(periode: Datoperiode): BigDecimal =
        perioder.single { kgPeriode -> kgPeriode.gjelderFor(periode) }
            .feilutbetaltYtelsesbeløp()

    fun datoperioder(eksternFagsakRevurdering: EksternFagsakRevurdering) = perioder.map { eksternFagsakRevurdering.utvidPeriode(it.periode) }

    fun feilutbetaltBeløpForAllePerioder() = perioder.sumOf { it.feilutbetaltYtelsesbeløp() }

    override fun perioder(): List<KravgrunnlagPeriodeAdapter> {
        return perioder
    }

    fun tilEntity(tilbakekrevingId: String): KravgrunnlagHendelseEntity {
        return KravgrunnlagHendelseEntity(
            id = id,
            tilbakekrevingId = tilbakekrevingId,
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
            perioder = perioder.map { it.tilEntity(id) },
        )
    }

    class Periode(
        private val id: UUID,
        val periode: Datoperiode,
        private val månedligSkattebeløp: BigDecimal,
        private val beløp: List<Beløp>,
    ) : KravgrunnlagPeriodeAdapter {
        fun gjelderFor(other: Datoperiode): Boolean = other.inneholder(periode)

        override fun periode(): Datoperiode {
            return periode
        }

        override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> {
            return beløp
        }

        override fun feilutbetaltYtelsesbeløp(): BigDecimal {
            return beløp.filter { it.erYtelsesbeløp() }.sumOf { it.tilbakekrevesBeløp }
        }

        fun tilEntity(kravgrunnlagId: UUID): KravgrunnlagPeriodeEntity {
            return KravgrunnlagPeriodeEntity(
                id = id,
                kravgrunnlagId = kravgrunnlagId,
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                månedligSkattebeløp = månedligSkattebeløp,
                beløp = beløp.map { it.tilEntity(id) },
            )
        }

        data class Beløp(
            private val id: UUID,
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

            fun erYtelsesbeløp(): Boolean = klassetype == "YTEL"

            fun tilEntity(kravgrunnlagPeriodeId: UUID): BeløpEntity {
                return BeløpEntity(
                    id = id,
                    kravgrunnlagPeriodeId = kravgrunnlagPeriodeId,
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
        val oppdragKode: String,
        val navn: String,
    ) {
        ANNULERT("ANNU", "Kravgrunnlag annullert"),
        ANNULLERT_OMG("ANOM", "Kravgrunnlag annullert ved omg"),
        AVSLUTTET("AVSL", "Avsluttet kravgrunnlag"),
        BEHANDLET("BEHA", "Kravgrunnlag ferdigbehandlet"),
        ENDRET("ENDR", "Endret kravgrunnlag"),
        FEIL("FEIL", "Feil på kravgrunnlag"),
        MANUELL("MANU", "Manuell behandling"),
        NY("NY", "Nytt kravgrunnlag"),
        SPERRET("SPER", "Kravgrunnlag sperret"),
        ;

        companion object {
            fun forOppdragKode(kode: String) = entries.single { it.oppdragKode == kode }
        }
    }
}
