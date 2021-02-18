package no.nav.familie.tilbake.sikkerhet


enum class Behandlerrolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0)
}

class InnloggetBrukertilgang(val tilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>)
