package no.nav.tilbakekreving.auth

enum class Approlle(private val entraRoleName: String) {
    Fagsystem("fagsystem"),
    ;

    companion object {
        fun roller(rollenavn: List<String>): List<Approlle> {
            return rollenavn.mapNotNull { navn -> Approlle.entries.firstOrNull { it.entraRoleName == navn } }
        }
    }
}
