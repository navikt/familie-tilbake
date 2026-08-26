package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.KravgrunnlagForskjellDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NyPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import java.math.BigDecimal
import java.util.UUID

class KravgrunnlagSammenligning(
    originaltKravgrunnlag: KravgrunnlagHendelse,
    nyttKravgrunnlag: KravgrunnlagHendelse,
    sporing: Sporing,
) {
    private val forskjeller: MutableList<Forskjell> = mutableListOf()

    internal fun resultat() = forskjeller.filterNot { it is Forskjell.UendretPeriode }

    fun sammendrag() = forskjeller
        .asSequence()
        .sortedBy { it.periode }
        .fold(listOf<Forskjell>()) { acc, forskjell ->
            when (val sammenslått = acc.lastOrNull()?.slåSammen(forskjell)) {
                null -> acc + forskjell
                else -> {
                    acc.dropLast(1) + sammenslått
                }
            }
        }
        .mapNotNull(Forskjell::tilDto)
        .filter { it !is EndretPeriodeDto || it.endringIBeløp != 0 }
        .toList()

    internal fun oppdaterSteg(steg: List<Saksbehandlingsteg>) {
        forskjeller.forEach { forskjell ->
            steg.forEach { forskjell.oppdater(it) }
        }
    }

    init {
        val originalePerioder = originaltKravgrunnlag.perioder().map { it.periode() }
        val perioderFraNyttKravgrunnlag = nyttKravgrunnlag.perioder().map { it.periode() }
        val ukjentePerioder = perioderFraNyttKravgrunnlag.filter { it !in originalePerioder }

        val feil = when {
            ukjentePerioder.any { ny -> originalePerioder.any { ny.overlapper(it) } } -> UtenforScope.KravgrunnlagMedUlikePerioder
            originalePerioder.size > perioderFraNyttKravgrunnlag.size -> UtenforScope.KravgrunnlagMedFærrePerioder
            !originaltKravgrunnlag.harNokOverlapp(nyttKravgrunnlag) -> UtenforScope.KravgrunnlagMedUlikeVerdier
            else -> null
        }
        if (feil != null) {
            throw ModellFeil.UtenforScopeException(feil, sporing)
        }

        forskjeller += nyttKravgrunnlag.perioder().map { nyPeriode ->
            val endringIBeløp = originaltKravgrunnlag.perioder()
                .firstOrNull { it.periode() == nyPeriode.periode() }
                ?.let { nyPeriode.feilutbetaltYtelsesbeløp() - it.feilutbetaltYtelsesbeløp() }
            when {
                endringIBeløp == null -> Forskjell.NyPeriode(nyPeriode.periode(), nyPeriode.feilutbetaltYtelsesbeløp())
                endringIBeløp.signum() == 0 -> Forskjell.UendretPeriode(nyPeriode.periode())
                else -> Forskjell.JustertBeløp(nyPeriode.periode(), endringIBeløp)
            }
        }
    }

    sealed interface Forskjell {
        val periode: Datoperiode
        val endringIBeløp: BigDecimal

        fun oppdater(steg: EndretKravgrunnlagObservatør)

        fun slåSammen(other: Forskjell): Forskjell?

        fun tilEntity(
            faktavurderingPeriodeRef: UUID?,
            vilkårsvurderingPeriodeRef: UUID?,
            foreldelsesvurderingPeriodeRef: UUID?,
        ): ForskjellEntity

        fun tilDto(): KravgrunnlagForskjellDto?

        data class JustertBeløp(override val periode: Datoperiode, override val endringIBeløp: BigDecimal) : Forskjell {
            override fun oppdater(steg: EndretKravgrunnlagObservatør) {
                steg.perideEndretBeløp(this)
            }

            override fun tilEntity(
                faktavurderingPeriodeRef: UUID?,
                vilkårsvurderingPeriodeRef: UUID?,
                foreldelsesvurderingPeriodeRef: UUID?,
            ): ForskjellEntity {
                return ForskjellEntity(
                    id = UUID.randomUUID(),
                    faktavurderingPeriodeRef = faktavurderingPeriodeRef,
                    vilkårsvurderingPeriodeRef = vilkårsvurderingPeriodeRef,
                    foreldelsesvurderingPeriodeRef = foreldelsesvurderingPeriodeRef,
                    type = ForskjellType.JustertBeløp,
                    originalPeriode = DatoperiodeEntity(periode.fom, periode.tom),
                    nyPeriode = null,
                    endringIBeløp = endringIBeløp,
                )
            }

            override fun slåSammen(other: Forskjell): Forskjell? {
                return when (other) {
                    is UendretPeriode -> this
                    is JustertBeløp -> JustertBeløp(
                        periode = this.periode.fom til other.periode.tom,
                        endringIBeløp = this.endringIBeløp + other.endringIBeløp,
                    )
                    else -> null
                }
            }

            override fun tilDto(): KravgrunnlagForskjellDto = EndretPeriodeDto(periode.fom, periode.tom, endringIBeløp.toInt())
        }

        data class NyPeriode(override val periode: Datoperiode, val beløp: BigDecimal) : Forskjell {
            override val endringIBeløp: BigDecimal = BigDecimal.ZERO

            override fun oppdater(steg: EndretKravgrunnlagObservatør) {
                steg.nyPeriode(this)
            }

            override fun tilEntity(
                faktavurderingPeriodeRef: UUID?,
                vilkårsvurderingPeriodeRef: UUID?,
                foreldelsesvurderingPeriodeRef: UUID?,
            ): ForskjellEntity {
                return ForskjellEntity(
                    id = UUID.randomUUID(),
                    faktavurderingPeriodeRef = faktavurderingPeriodeRef,
                    vilkårsvurderingPeriodeRef = vilkårsvurderingPeriodeRef,
                    foreldelsesvurderingPeriodeRef = foreldelsesvurderingPeriodeRef,
                    type = ForskjellType.NyPeriode,
                    originalPeriode = null,
                    nyPeriode = DatoperiodeEntity(periode.fom, periode.tom),
                    endringIBeløp = beløp,
                )
            }

            override fun slåSammen(other: Forskjell): Forskjell? {
                return when (other) {
                    is NyPeriode -> NyPeriode(periode.fom til other.periode.tom, beløp + other.beløp)
                    else -> null
                }
            }

            override fun tilDto(): KravgrunnlagForskjellDto = NyPeriodeDto(periode.fom, periode.tom, beløp.toInt())
        }

        data class UendretPeriode(override val periode: Datoperiode) : Forskjell {
            override val endringIBeløp: BigDecimal = BigDecimal.ZERO

            override fun oppdater(steg: EndretKravgrunnlagObservatør) {}

            override fun tilEntity(
                faktavurderingPeriodeRef: UUID?,
                vilkårsvurderingPeriodeRef: UUID?,
                foreldelsesvurderingPeriodeRef: UUID?,
            ): ForskjellEntity = throw IllegalStateException("UendretPeriode skal ikke persisteres")

            override fun slåSammen(other: Forskjell) = other

            override fun tilDto(): KravgrunnlagForskjellDto? = null
        }
    }

    enum class ForskjellType {
        JustertBeløp,
        NyPeriode,
    }
}
