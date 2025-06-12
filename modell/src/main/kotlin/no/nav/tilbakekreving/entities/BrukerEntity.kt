package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.person.Bruker
import java.time.LocalDate

@Serializable
data class BrukerEntity(
    val ident: String,
    var språkkode: String? = null,
    var navn: String? = null,
    var fødselsdato: String? = null,
    var kjønn: String? = null,
    var dødsdato: String? = null,
) {
    fun fraEntity(): Bruker {
        println("====>>> dødsdato: $dødsdato")
        return Bruker(
            ident = ident,
            språkkode = språkkode?.let { Språkkode.valueOf(it) },
            navn = navn,
            fødselsdato = fødselsdato?.let { LocalDate.parse(fødselsdato!!) },
            kjønn = kjønn?.let { Kjønn.valueOf(it) },
            dødsdato = dødsdato?.let { LocalDate.parse(it) },
        )
    }
}
