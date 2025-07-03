package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker

object BrevmottakerAdresseValidering {
    fun erBrevmottakereGyldige(brevmottakere: List<ManuellBrevmottaker>): Boolean =
        brevmottakere.all(ManuellBrevmottaker::erGyldig)
}
