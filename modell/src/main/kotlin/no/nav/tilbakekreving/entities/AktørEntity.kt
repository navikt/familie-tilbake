package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse

data class AktørEntity(
    val aktørType: AktørType,
    val ident: String,
) {
    fun fraEntity(): KravgrunnlagHendelse.Aktør {
        return when (aktørType) {
            AktørType.Person -> KravgrunnlagHendelse.Aktør.Person(ident)
            AktørType.Organisasjon -> KravgrunnlagHendelse.Aktør.Organisasjon(ident)
            AktørType.Samhandler -> KravgrunnlagHendelse.Aktør.Samhandler(ident)
            AktørType.Applikasjonsbruker -> KravgrunnlagHendelse.Aktør.Applikasjonsbruker(ident)
        }
    }
}

enum class AktørType {
    Person,
    Organisasjon,
    Samhandler,
    Applikasjonsbruker,
}
