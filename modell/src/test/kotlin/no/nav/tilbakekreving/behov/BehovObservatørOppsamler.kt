package no.nav.tilbakekreving.behov

import io.kotest.matchers.nulls.shouldNotBeNull

class BehovObservatørOppsamler() : BehovObservatør {
    val behovListe = mutableListOf<Behov>()

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

    fun sisteVarselbrevId() = behovListe.filterIsInstance<VarselbrevBehov>().lastOrNull().shouldNotBeNull().brevId
}
