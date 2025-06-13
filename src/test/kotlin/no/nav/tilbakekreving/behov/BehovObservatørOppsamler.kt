package no.nav.tilbakekreving.behov

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
}
