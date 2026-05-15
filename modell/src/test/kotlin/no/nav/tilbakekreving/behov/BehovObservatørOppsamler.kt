package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse

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

    fun journalføringEventFor(): VarselbrevJournalføringHendelse {
        return VarselbrevJournalføringHendelse(
            journalpostId = "",
            dokumentInfoId = "",
            varselbrevId = (behovListe.last() as VarselbrevJournalføringBehov).brevId,
        )
    }

    fun distribuerHendelseFor(): VarselbrevDistribueringHendelse {
        return VarselbrevDistribueringHendelse(
            journalpostId = "",
            dokumentInfoId = "",
            brevId = (behovListe.last() as VarselbrevDistribusjonBehov).brevId,
        )
    }
}
