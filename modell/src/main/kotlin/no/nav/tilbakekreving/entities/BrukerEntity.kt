package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.person.Bruker
import java.time.LocalDate

data class BrukerEntity(
    val ident: String,
    var språkkode: String? = null,
    var navn: String? = null,
    var fødselsdato: LocalDate? = null,
    var kjønn: String? = null,
    var dødsdato: LocalDate? = null,
) {
    fun fraEntity(): Bruker {
        return Bruker(
            ident = ident,
            språkkode = språkkode?.let { Språkkode.valueOf(it) },
            navn = navn,
            fødselsdato = fødselsdato,
            kjønn = kjønn?.let { Kjønn.valueOf(it) },
            dødsdato = dødsdato,
        )
    }
}
