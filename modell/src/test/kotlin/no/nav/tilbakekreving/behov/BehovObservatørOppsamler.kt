package no.nav.tilbakekreving.behov

class BehovObservatørOppsamler : BehovObservatør {
    private val _fagsysteminfoBehov = mutableListOf<FagsysteminfoBehov>()
    val fagsysteminfoBehov: List<FagsysteminfoBehov> get() = _fagsysteminfoBehov

    private val _varselbrevBehov = mutableListOf<VarselbrevBehov>()
    val varselbrevBehov: List<VarselbrevBehov> get() = _varselbrevBehov

    override fun håndter(behov: FagsysteminfoBehov) {
        _fagsysteminfoBehov.add(behov)
    }

    override fun håndter(behov: VarselbrevBehov) {
        _varselbrevBehov.add(behov)
    }
}
