package no.nav.tilbakekreving.breeeev.standardtekster.bortfaltKravgrunnlag

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

enum class KravBortfaltTekster(
    val tittel: String,
    private val avsnitt: (Ytelse, Språkkode) -> Array<String>,
) {
    INNLEDNING(
        tittel = "Kravgrunnlag bortfalt",
        avsnitt = { ytelse, språkkode ->
            arrayOf(
                "Vi informerte deg om at du hadde fått for mye utbetalt i stønad ${ytelse.hentYtelsesnavn(språkkode)}",
                "vi har i ettertid avsluttet saken om tilbakebetalingen fordi det ikke er et feilutbetalt beløp å betale tilbake",
            )
        },
    ),
    SPØRSMÅL(
        tittel = "Har du spørsmål?",
        avsnitt = { ytelse, språkkode ->
            arrayOf(
                "Du finner mer informasjon på ${ytelse.brevmeta().url}. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00–15.00.",
            )
        },
    ),
    ;

    fun byggAvsnitt(ytelse: Ytelse, språkkode: Språkkode): Set<String> {
        return avsnitt(ytelse, språkkode).toSet()
    }

    companion object {
        fun finnTekster(ytelse: Ytelse, språkkode: Språkkode): Set<String> {
            return entries.flatMap { it.byggAvsnitt(ytelse, språkkode) }.toSet()
        }
    }
}
