package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.saksbehandler.Behandler

object FellesTestdata {
    const val SAKSBEHANDLER_IDENT = "Z999999"
    const val BESLUTTER_IDENT = "Z111111"

    val ANSVARLIG_SAKSBEHANDLER = Behandler.Saksbehandler(SAKSBEHANDLER_IDENT)
    val ANSVARLIG_BESLUTTER = Behandler.Saksbehandler(BESLUTTER_IDENT)
}
