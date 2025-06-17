package no.nav.tilbakekreving

import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov

class Observatør() : BehovObservatør {
    private val behovListe = mutableListOf<Behov>()

    fun harUbesvarteBehov() = behovListe.isNotEmpty()

    fun nesteBehov() = behovListe.removeAt(0)

    override fun håndter(
        behov: FagsysteminfoBehov,
    ) {
        behovListe.add(behov)
    }

    override fun håndter(
        behov: VarselbrevBehov,
    ) {
        behovListe.add(behov)
    }

    override fun håndter(
        behov: BrukerinfoBehov,
    ) {
        behovListe.add(behov)
    }

    override fun håndter(behov: IverksettelseBehov) {
        behovListe.add(behov)
    }
}
