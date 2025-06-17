package no.nav.tilbakekreving.behov

interface BehovObservatør {
    fun håndter(
        behov: FagsysteminfoBehov,
    )

    fun håndter(
        behov: VarselbrevBehov,
    )

    fun håndter(
        behov: BrukerinfoBehov,
    )

    fun håndter(behov: IverksettelseBehov)
}
