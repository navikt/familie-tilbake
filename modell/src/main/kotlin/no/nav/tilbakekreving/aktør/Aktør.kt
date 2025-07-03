package no.nav.tilbakekreving.aktør

import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType

sealed interface Aktør {
    val ident: String

    fun tilEntity(): AktørEntity

    data class Person(override val ident: String) : Aktør {
        override fun tilEntity(): AktørEntity {
            return AktørEntity(AktørType.Person, ident)
        }
    }

    data class Organisasjon(override val ident: String) : Aktør {
        override fun tilEntity(): AktørEntity {
            return AktørEntity(AktørType.Organisasjon, ident)
        }
    }

    data class Samhandler(override val ident: String) : Aktør {
        override fun tilEntity(): AktørEntity {
            return AktørEntity(AktørType.Samhandler, ident)
        }
    }

    data class Applikasjonsbruker(override val ident: String) : Aktør {
        override fun tilEntity(): AktørEntity {
            return AktørEntity(AktørType.Applikasjonsbruker, ident)
        }
    }
}
