package no.nav.tilbakekreving.behov

interface BehovObservatør {
    fun håndter(
        behov: FagsysteminfoBehov,
    )

    fun håndter(
        behov: VarselbrevJournalføringBehov,
    )

    fun håndter(
        behov: VarselbrevDistribusjonBehov,
    )

    fun håndter(
        behov: BrukerinfoBehov,
    )

    fun håndter(behov: IverksettelseBehov)

    fun håndter(behov: VedtaksbrevJournalføringBehov)

    fun håndter(behov: VedtaksbrevDistribusjonBehov)
}
