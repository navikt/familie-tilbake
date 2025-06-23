package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.person.Bruker
import java.time.LocalDate

data class BrukerEntity(
    val ident: String,
    var språkkode: Språkkode?,
    var navn: String?,
    var fødselsdato: LocalDate?,
    var kjønn: Kjønn?,
    var dødsdato: LocalDate?,
) {
    fun fraEntity(): Bruker {
        return Bruker(
            ident = ident,
            språkkode = språkkode,
            navn = navn,
            fødselsdato = fødselsdato,
            kjønn = kjønn,
            dødsdato = dødsdato,
        )
    }
}
