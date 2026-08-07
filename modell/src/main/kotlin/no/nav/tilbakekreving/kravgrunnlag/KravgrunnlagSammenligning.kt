package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class KravgrunnlagSammenligning(
    originaltKravgrunnlag: KravgrunnlagHendelse,
    nyttKravgrunnlag: KravgrunnlagHendelse,
    sporing: Sporing,
) {
    private val forskjeller: List<Forskjell>

    fun resultat() = forskjeller

    fun endringIBeløp() = forskjeller.sumOf { it.endringIBeløp }

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

        data class JustertBeløp(val periode: Datoperiode, override val endringIBeløp: BigDecimal) : Forskjell
    }
}
