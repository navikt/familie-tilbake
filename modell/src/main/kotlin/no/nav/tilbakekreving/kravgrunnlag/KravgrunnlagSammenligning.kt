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
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
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
                else -> acc.dropLast(1) + sammenslått
            }
        }
        .mapNotNull(Forskjell::tilDto)
        .filter { it !is EndretPeriodeDto || it.gammeltBeløp != it.nyttBeløp }
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
            ukjentePerioder.any { ny -> originalePerioder.count { ny.overlapper(it) } > 1 } -> UtenforScope.KravgrunnlagMedSammenslåttPerioder
            originalePerioder.size > perioderFraNyttKravgrunnlag.size -> UtenforScope.KravgrunnlagMedFærrePerioder
            !originaltKravgrunnlag.harNokOverlapp(nyttKravgrunnlag) -> UtenforScope.KravgrunnlagMedUlikeVerdier
            else -> null
        }
        if (feil != null) {
            throw ModellFeil.UtenforScopeException(feil, sporing)
        }

        forskjeller += nyttKravgrunnlag.perioder().map { nyPeriode ->
            val eksisterendePeriode = originaltKravgrunnlag.perioder()
                .firstOrNull { nyPeriode.periode().overlapper(it.periode()) }
            when {
                eksisterendePeriode == null -> Forskjell.NyPeriode(nyPeriode.periode(), nyPeriode.feilutbetaltYtelsesbeløp())
                nyPeriode.feilutbetaltYtelsesbeløp().toInt() != eksisterendePeriode.feilutbetaltYtelsesbeløp().toInt() ||
                    nyPeriode.periode() != eksisterendePeriode.periode() -> Forskjell.EndretPeriode(
                    periode = eksisterendePeriode.periode(),
                    nyPeriode = nyPeriode.periode().takeIf { it != eksisterendePeriode.periode() },
                    gammeltBeløp = eksisterendePeriode.feilutbetaltYtelsesbeløp(),
                    nyttBeløp = nyPeriode.feilutbetaltYtelsesbeløp(),
                    etterfølgende = null,
                )

                else -> Forskjell.UendretPeriode(
                    periode = nyPeriode.periode(),
                    gammeltBeløp = eksisterendePeriode.feilutbetaltYtelsesbeløp(),
                    nyttBeløp = nyPeriode.feilutbetaltYtelsesbeløp(),
                )
            }
        }
    }

    sealed interface Forskjell {
        val periode: Datoperiode
        val nyttBeløp: BigDecimal

        fun oppdater(steg: EndretKravgrunnlagObservatør)

        fun slåSammen(other: Forskjell): Forskjell?

        fun tilEntity(
            faktavurderingPeriodeRef: UUID?,
            vilkårsvurderingPeriodeRef: UUID?,
            foreldelsesvurderingPeriodeRef: UUID?,
        ): ForskjellEntity

        fun tilDto(): KravgrunnlagForskjellDto?

        data class EndretPeriode(
            override val periode: Datoperiode,
            val nyPeriode: Datoperiode?,
            val gammeltBeløp: BigDecimal,
            override val nyttBeløp: BigDecimal,
            private val etterfølgende: UendretPeriode?,
        ) : Forskjell {
            override fun oppdater(steg: EndretKravgrunnlagObservatør) {
                steg.periodeEndret(this)
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
                    nyPeriode = nyPeriode?.let { DatoperiodeEntity(it.fom, it.tom) },
                    gammeltBeløp = gammeltBeløp,
                    nyttBeløp = nyttBeløp,
                )
            }

            override fun slåSammen(other: Forskjell): Forskjell? {
                return when (other) {
                    is UendretPeriode -> EndretPeriode(
                        periode = periode,
                        nyPeriode = nyPeriode,
                        gammeltBeløp = gammeltBeløp,
                        nyttBeløp = nyttBeløp,
                        etterfølgende = etterfølgende?.slåSammen(other) ?: other,
                    )

                    is EndretPeriode -> EndretPeriode(
                        periode = this.periode.fom til other.periode.tom,
                        nyPeriode = when {
                            nyPeriode != null -> nyPeriode.fom til (other.nyPeriode?.tom ?: other.periode.tom)
                            other.nyPeriode != null -> periode.fom til other.nyPeriode.tom
                            else -> null
                        },
                        gammeltBeløp = this.gammeltBeløp + other.gammeltBeløp + (etterfølgende?.gammeltBeløp ?: BigDecimal.ZERO),
                        nyttBeløp = this.nyttBeløp + other.nyttBeløp + (etterfølgende?.nyttBeløp ?: BigDecimal.ZERO),
                        etterfølgende = null,
                    )

                    else -> null
                }
            }

            override fun tilDto(): KravgrunnlagForskjellDto = EndretPeriodeDto(
                fom = nyPeriode?.fom ?: periode.fom,
                tom = nyPeriode?.tom ?: periode.tom,
                gammelPeriode = PeriodeDto(periode.fom, periode.tom),
                gammeltBeløp = gammeltBeløp.toInt(),
                nyttBeløp = nyttBeløp.toInt(),
            )
        }

        data class NyPeriode(override val periode: Datoperiode, override val nyttBeløp: BigDecimal) : Forskjell {
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
                    gammeltBeløp = null,
                    nyttBeløp = nyttBeløp,
                )
            }

            override fun slåSammen(other: Forskjell): Forskjell? {
                return when (other) {
                    is NyPeriode -> NyPeriode(periode.fom til other.periode.tom, nyttBeløp + other.nyttBeløp)
                    else -> null
                }
            }

            override fun tilDto(): KravgrunnlagForskjellDto = NyPeriodeDto(periode.fom, periode.tom, nyttBeløp.toInt())
        }

        data class UendretPeriode(
            override val periode: Datoperiode,
            override val nyttBeløp: BigDecimal,
            val gammeltBeløp: BigDecimal,
        ) : Forskjell {
            override fun oppdater(steg: EndretKravgrunnlagObservatør) {}

            override fun tilEntity(
                faktavurderingPeriodeRef: UUID?,
                vilkårsvurderingPeriodeRef: UUID?,
                foreldelsesvurderingPeriodeRef: UUID?,
            ): ForskjellEntity = throw IllegalStateException("UendretPeriode skal ikke persisteres")

            override fun slåSammen(other: Forskjell) = when (other) {
                is UendretPeriode -> UendretPeriode(
                    periode.fom til other.periode.tom,
                    nyttBeløp + other.nyttBeløp,
                    gammeltBeløp + other.gammeltBeløp,
                )

                else -> null
            }

            override fun tilDto(): KravgrunnlagForskjellDto? = null
        }
    }

    enum class ForskjellType {
        JustertBeløp,
        NyPeriode,
    }
}
