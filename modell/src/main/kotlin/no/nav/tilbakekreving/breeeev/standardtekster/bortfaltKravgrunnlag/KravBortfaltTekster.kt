package no.nav.tilbakekreving.breeeev.standardtekster.bortfaltKravgrunnlag

import no.nav.tilbakekreving.fagsystem.Ytelse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATOFORMAT = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.of("nb"))

enum class KravBortfaltTekster(
    val tittel: String,
    private val avsnittBuilder: (Ytelse, LocalDate) -> Array<String>,
) {
    INNLEDNING(
        tittel = "Nav har avsluttet saken din om tilbakebetaling",
        avsnittBuilder = { ytelse, varsletDato ->
            arrayOf(
                "Vi sendte deg et varsel ${DATOFORMAT.format(varsletDato)} om at du hadde fått for mye utbetalt i ${ytelse.brevmeta().ubestemtEntall}. " +
                    "Vi har i ettertid avsluttet saken om tilbakebetaling fordi det ikke lenger er et feilutbetalt beløp å betale tilbake.",
            )
        },
    ),
    SPØRSMÅL(
        tittel = "Har du spørsmål?",
        avsnittBuilder = { ytelse, _ ->
            arrayOf(
                "Du finner mer informasjon på ${ytelse.brevmeta().url}. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00–15.00.",
            )
        },
    ),
    ;

    fun avsnitt(ytelse: Ytelse, varsletDato: LocalDate): Array<String> {
        return avsnittBuilder(ytelse, varsletDato)
    }

    companion object {
        val STANDARD_BUNNTEKSTER = arrayOf(
            SPØRSMÅL,
        )
    }
}
