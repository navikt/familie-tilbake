package no.nav.tilbakekreving.person

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.kontrakter.bruker.FrontendBrukerDto
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import java.time.LocalDate

class Bruker(
    private val ident: String,
    val språkkode: Språkkode,
    private var navn: String? = null,
    private var fødselsdato: LocalDate? = null,
    private var kjønn: Kjønn? = null,
    private var dødsdato: LocalDate? = null,
) : FrontendDto<FrontendBrukerDto> {
    override fun tilFrontendDto(): FrontendBrukerDto {
        return FrontendBrukerDto(
            personIdent = ident,
            navn = navn ?: "Ukjent",
            fødselsdato = fødselsdato,
            kjønn = kjønn ?: Kjønn.UKJENT,
            dødsdato = dødsdato,
        )
    }

    companion object {
        fun Bruker?.tilNullableFrontendDto(): FrontendBrukerDto {
            return this?.tilFrontendDto() ?: FrontendBrukerDto(
                personIdent = "Ukjent",
                navn = "Ukjent",
                fødselsdato = null,
                kjønn = Kjønn.UKJENT,
                dødsdato = null,
            )
        }
    }
}
