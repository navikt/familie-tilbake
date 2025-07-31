package no.nav.tilbakekreving.auth

sealed class Authentication {
    class Systembruker(
        val roles: List<Approlle>,
    ) : Authentication() {
        fun harRolle(rolle: Approlle) {
            roles.contains(rolle)
        }
    }
}
