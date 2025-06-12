package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse

@Serializable
data class AktørEntity(
    val aktørType: String,
    val ident: String,
) {
    fun fraEntity(): KravgrunnlagHendelse.Aktør {
        val aktør = when {
            aktørType.equals("Person") -> KravgrunnlagHendelse.Aktør.Person(ident)
            aktørType.equals("Organisasjon") -> KravgrunnlagHendelse.Aktør.Organisasjon(ident)
            aktørType.equals("Samhandler") -> KravgrunnlagHendelse.Aktør.Samhandler(ident)
            aktørType.equals("Applikasjonsbruker") -> KravgrunnlagHendelse.Aktør.Applikasjonsbruker(ident)
            else -> throw IllegalArgumentException("ugildig aktør type: $aktørType")
        }
        return aktør
    }
}
