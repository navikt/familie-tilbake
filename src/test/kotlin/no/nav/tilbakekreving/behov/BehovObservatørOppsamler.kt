package no.nav.tilbakekreving.behov

class BehovObservatørOppsamler() : BehovObservatør {
    val behovListe = mutableListOf<Behov>()

    override fun håndter(
        behov: FagsysteminfoBehov,
    ) {
        behovListe.add(behov)
    }

    override fun håndter(
        behov: VarselbrevJournalføringBehov,
    ) {
        behovListe.add(behov)
    }

    override fun håndter(
        behov: VarselbrevDistribusjonBehov,
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

    override fun håndter(behov: VedtaksbrevJournalføringBehov) {
        behovListe.add(behov)
    }

    override fun håndter(behov: VedtaksbrevDistribusjonBehov) {
        behovListe.add(behov)
    }
}
