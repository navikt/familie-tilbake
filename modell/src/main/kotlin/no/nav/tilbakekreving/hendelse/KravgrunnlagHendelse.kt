package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.behov.KravgrunnlagInfo
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
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
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
    val referanse: String,
    private val perioder: List<Periode>,
    val korrigering: Boolean,
    override val opprettet: LocalDateTime,
) : Historikk.HistorikkInnslag<UUID>, KravgrunnlagAdapter {
    fun valider(sporing: Sporing) {
        if (vedtakGjelder !is Aktør.Person || utbetalesTil !is Aktør.Person) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagIkkePerson, sporing)
        }

        if (vedtakGjelder.ident != utbetalesTil.ident) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBrukerIkkeLikMottaker, sporing)
        }

        if (kravstatuskode !in arrayOf(Kravstatuskode.NY, Kravstatuskode.ENDRET)) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttet, sporing)
        }
    }

    fun skalOppretteNySak() = kravstatuskode == Kravstatuskode.NY

    fun totaltBeløpFor(
        periode: Datoperiode,
        revurdering: EksternFagsakRevurdering,
    ): BigDecimal = perioder.filter { revurdering.utvidPeriode(it.periode) in periode }.sumOf { it.feilutbetaltYtelsesbeløp() }

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
            opprettet = opprettet,
            korrigering = korrigering,
        )
    }

    fun hentKravgrunnlaginfoForIverksettelse(): KravgrunnlagInfo =
        KravgrunnlagInfo(kontrollfelt = kontrollfelt)

    fun kanBrukesUtenNyVurdering(other: KravgrunnlagHendelse): Boolean {
        return this === other ||
            this.harNokOverlapp(other) &&
            this.skalBeregneRenter == other.skalBeregneRenter &&
            this.perioder == other.perioder
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
            other is KravgrunnlagHendelse &&
            kanBrukesUtenNyVurdering(other) &&
            vedtakId == other.vedtakId &&
            kravstatuskode == other.kravstatuskode &&
            fagsystemVedtaksdato == other.fagsystemVedtaksdato &&
            vedtakGjelder == other.vedtakGjelder &&
            utbetalesTil == other.utbetalesTil &&
            ansvarligEnhet == other.ansvarligEnhet &&
            kontrollfelt == other.kontrollfelt &&
            kravgrunnlagId == other.kravgrunnlagId &&
            referanse == other.referanse
    }

    override fun hashCode(): Int {
        return Objects.hash(
            vedtakId,
            kravstatuskode,
            fagsystemVedtaksdato,
            vedtakGjelder,
            utbetalesTil,
            skalBeregneRenter,
            ansvarligEnhet,
            kontrollfelt,
            kravgrunnlagId,
            referanse,
            perioder,
        )
    }

    fun sammenlign(
        nyttKravgrunnlag: KravgrunnlagHendelse,
        sporing: Sporing,
    ): KravgrunnlagSammenligning = KravgrunnlagSammenligning(this, nyttKravgrunnlag, sporing)

    fun harNokOverlapp(other: KravgrunnlagHendelse): Boolean = this.vedtakId == other.vedtakId ||
        this.vedtakGjelder == other.vedtakGjelder ||
        this.utbetalesTil == other.utbetalesTil ||
        this.kravgrunnlagId == other.kravgrunnlagId

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

        override fun equals(other: Any?): Boolean {
            return this === other ||
                other is Periode &&
                periode == other.periode &&
                månedligSkattebeløp == other.månedligSkattebeløp &&
                beløp == other.beløp
        }

        override fun hashCode(): Int {
            return Objects.hash(periode, månedligSkattebeløp, beløp)
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

            override fun equals(other: Any?): Boolean {
                return this === other ||
                    other is Beløp &&
                    this.klassekode == other.klassekode &&
                    this.klassetype == other.klassetype &&
                    this.opprinneligUtbetalingsbeløp == other.opprinneligUtbetalingsbeløp &&
                    this.nyttBeløp == other.nyttBeløp &&
                    this.tilbakekrevesBeløp == other.tilbakekrevesBeløp &&
                    this.skatteprosent == other.skatteprosent
            }

            override fun hashCode(): Int {
                return Objects.hash(klassekode, klassetype, opprinneligUtbetalingsbeløp, nyttBeløp, tilbakekrevesBeløp, skatteprosent)
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
