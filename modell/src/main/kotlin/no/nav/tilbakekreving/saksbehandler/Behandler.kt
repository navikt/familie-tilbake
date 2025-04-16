package no.nav.tilbakekreving.saksbehandler

sealed interface Behandler {
    val ident: String

    class Saksbehandler(override val ident: String) : Behandler {
        override fun equals(other: Any?): Boolean {
            return ident == (other as? Saksbehandler)?.ident
        }

        override fun hashCode(): Int = ident.hashCode()
    }

    data object Vedtaksløsning : Behandler {
        override val ident: String = "VL"
    }
}
