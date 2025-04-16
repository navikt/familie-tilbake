package no.nav.tilbakekreving.saksbehandler

interface Saksbehandling {
    fun oppdaterAnsvarligSaksbehandler(behandler: Behandler)

    fun ansvarligSaksbehandler(): Behandler
}
