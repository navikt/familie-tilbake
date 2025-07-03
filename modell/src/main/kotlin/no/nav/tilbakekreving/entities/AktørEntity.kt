package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.aktør.Aktør

data class AktørEntity(
    val aktørType: AktørType,
    val ident: String,
) {
    fun fraEntity(): Aktør {
        return when (aktørType) {
            AktørType.Person -> Aktør.Person(ident)
            AktørType.Organisasjon -> Aktør.Organisasjon(ident)
            AktørType.Samhandler -> Aktør.Samhandler(ident)
            AktørType.Applikasjonsbruker -> Aktør.Applikasjonsbruker(ident)
        }
    }
}

enum class AktørType {
    Person,
    Organisasjon,
    Samhandler,
    Applikasjonsbruker,
}
