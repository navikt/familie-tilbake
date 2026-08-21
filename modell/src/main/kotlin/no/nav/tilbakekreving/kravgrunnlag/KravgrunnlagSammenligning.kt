package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
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

    internal fun resultat() = forskjeller

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

        forskjeller += ukjentePerioder.map { Forskjell.NyPeriode(it.fom til it.tom) }

        forskjeller += originaltKravgrunnlag.perioder().zip(nyttKravgrunnlag.perioder())
            .map { (a, b) -> Forskjell.JustertBeløp(a.periode(), b.feilutbetaltYtelsesbeløp() - a.feilutbetaltYtelsesbeløp()) }
            .filter { it.endringIBeløp != BigDecimal.ZERO }
    }

    sealed interface Forskjell {
        val endringIBeløp: BigDecimal

        fun oppdater(steg: EndretKravgrunnlagObservatør)

        fun tilEntity(
            faktavurderingPeriodeRef: UUID?,
            vilkårsvurderingPeriodeRef: UUID?,
            foreldelsesvurderingPeriodeRef: UUID?,
        ): ForskjellEntity

        data class JustertBeløp(val periode: Datoperiode, override val endringIBeløp: BigDecimal) : Forskjell {
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
        }

        data class NyPeriode(val periode: Datoperiode) : Forskjell {
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
                    endringIBeløp = null,
                )
            }
        }
    }

    enum class ForskjellType {
        JustertBeløp,
        NyPeriode,
    }
}
