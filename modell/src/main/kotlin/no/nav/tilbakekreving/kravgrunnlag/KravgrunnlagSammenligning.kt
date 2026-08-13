package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.util.UUID

class KravgrunnlagSammenligning(
    originaltKravgrunnlag: KravgrunnlagHendelse,
    nyttKravgrunnlag: KravgrunnlagHendelse,
    sporing: Sporing,
) {
    private val forskjeller: List<Forskjell>

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
            ukjentePerioder.isNotEmpty() -> UtenforScope.KravgrunnlagMedNyePerioder
            originalePerioder.size != perioderFraNyttKravgrunnlag.size -> UtenforScope.KravgrunnlagMedFærrePerioder
            !originaltKravgrunnlag.harNokOverlapp(nyttKravgrunnlag) -> UtenforScope.KravgrunnlagMedUlikeVerdier
            else -> null
        }
        if (feil != null) {
            throw ModellFeil.UtenforScopeException(feil, sporing)
        }

        forskjeller = originaltKravgrunnlag.perioder().zip(nyttKravgrunnlag.perioder())
            .map { (a, b) -> Forskjell.JustertBeløp(a.periode(), b.feilutbetaltYtelsesbeløp() - a.feilutbetaltYtelsesbeløp()) }
    }

    sealed interface Forskjell {
        val endringIBeløp: BigDecimal

        fun oppdater(steg: EndretKravgrunnlagObservatør)

        fun tilEntity(
            faktavurderingPeriodeRef: UUID?,
            vilkårsvurderingPeriodeRef: UUID?,
        ): ForskjellEntity

        data class JustertBeløp(val periode: Datoperiode, override val endringIBeløp: BigDecimal) : Forskjell {
            override fun oppdater(steg: EndretKravgrunnlagObservatør) {
                steg.perideEndretBeløp(this)
            }

            override fun tilEntity(
                faktavurderingPeriodeRef: UUID?,
                vilkårsvurderingPeriodeRef: UUID?,
            ): ForskjellEntity {
                return ForskjellEntity(
                    id = UUID.randomUUID(),
                    faktavurderingPeriodeRef = faktavurderingPeriodeRef,
                    vilkårsvurderingPeriodeRef = vilkårsvurderingPeriodeRef,
                    originalPeriode = DatoperiodeEntity(periode.fom, periode.tom),
                    endringIBeløp = endringIBeløp,
                )
            }
        }
    }
}
