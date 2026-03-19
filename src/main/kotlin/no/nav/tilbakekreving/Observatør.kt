package no.nav.tilbakekreving

import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.DistribusjonBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.JournalføringBehov
import no.nav.tilbakekreving.behov.VarselbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov

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

    override fun håndter(behov: JournalføringBehov) {
        behovListe.add(behov)
    }

    override fun håndter(behov: DistribusjonBehov) {
        behovListe.add(behov)
    }
}
