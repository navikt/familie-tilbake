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

    init {
        if (originaltKravgrunnlag.perioder().map { it.periode() }.sorted() != nyttKravgrunnlag.perioder().map { it.periode() }.sorted()) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt, sporing)
        }
        if (!originaltKravgrunnlag.harNokOverlapp(nyttKravgrunnlag)) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt, sporing)
        }

        forskjeller = originaltKravgrunnlag.perioder().zip(nyttKravgrunnlag.perioder())
            .map { (a, b) -> Forskjell.JustertBeløp(a.periode(), b.feilutbetaltYtelsesbeløp() - a.feilutbetaltYtelsesbeløp()) }
    }

    sealed interface Forskjell {
        data class JustertBeløp(val periode: Datoperiode, val differanse: BigDecimal) : Forskjell
    }
}
