package no.nav.familie.tilbake.kontrakter

data class NavIdent(
    val ident: String,
) {
    init {
        if (ident.isBlank()) {
            throw IllegalArgumentException("Ident kan ikke være tom")
        }
    }
}
