package no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel

import no.nav.tilbakekreving.fagsystem.Ytelse

enum class ForhåndsvarselTekster(
    val tittel: String,
    private val avsnittBuilder: (Ytelse) -> Array<String>,
) {
    ÅRSAK_TIL_FEILUTBETALING(
        tittel = "Årsak til feilutbetaling",
        avsnittBuilder = { arrayOf("") },
    ),
    DETTE_LEGGES_VEKT_PÅ(
        tittel = "Dette legger vi vekt på i vurderingen vår",
        avsnittBuilder = {
            arrayOf(
                "For å avgjøre om vi kan kreve tilbake, tar vi først stilling til\n" +
                    "   •  om du forstod eller burde forstått at beløpet du fikk utbetalt var feil\n" +
                    "   •  om du har gitt riktig informasjon til Nav\n" +
                    "   •  om du har gitt all informasjon til Nav i rett tid",
                "Hvis resultatet blir at vi kan kreve tilbake, vurderer vi om du skal betale tilbake hele eller deler av beløpet. Da legger vi blant annet vekt på\n" +
                    "   •  hvor uaktsom du har vært\n" +
                    "   •  hvor lang tid det har gått siden #inputs.ytelsesnavn ble feilutbetalt\n" +
                    "   •  hvor stort det feilutbetalte beløpet er\n" +
                    "   •  om Nav har skyld i feilutbetalingen",
                "Hvis du må betale tilbake, og du har gitt oss feil eller mangelfull informasjon, kan vi kreve at du betaler et rentetillegg på ti prosent av beløpet",
                "Dette går fram av folketrygdloven §§ 22-15 og 22-17a.",
            )
        },
    ),
    FORELØPIG_VURDERING(
        tittel = "Vår foreløpige vurdering av saken din",
        avsnittBuilder = {
            arrayOf("Vi understreker at denne vurderingen ikke er endelig. Dette er et forhåndsvarsel etter forvaltningsloven § 16 om at vi vurderer å kreve tilbake det feilutbetalte beløpet. Dette er altså ikke et vedtak om tilbakekreving. Før Nav går videre i saksbehandlingen og avgjør saken din, har du muligheten til å uttale deg.")
        },
    ),
    HVORDAN_UTTALE_SEG(
        tittel = "Slik uttaler du deg",
        avsnittBuilder = {
            arrayOf(
                "Du kan sende uttalelsen din ved å logge deg inn på nav.no/skriv-til-oss og velge «Send beskjed til Nav». Du kan også sende uttalelsen din til oss i posten. Adressen finner du på nav.no/ettersendelser",
            )
        },
    ),
    RETT_TIL_INNSYN(
        tittel = "Du har rett til innsyn",
        avsnittBuilder = {
            arrayOf("Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.")
        },
    ),
    PERSONVÆRN_ERKLÆRING(
        tittel = "Du har rettigheter knyttet til personopplysningene dine",
        avsnittBuilder = {
            arrayOf("Du finner informasjon om hvordan Nav behandler personopplysningene dine, og hvilke rettigheter du har, på nav.no/personvernerklaering. ")
        },
    ),
    SPØRSMÅL(
        tittel = "Har du spørsmål?",
        avsnittBuilder = { ytelse ->
            arrayOf(
                "Du finner mer informasjon på ${ytelse.brevmeta().url}. På nav.no/kontakt kan du chatte eller skrive til oss. Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00–15.00.",
            )
        },
    ),
    ;

    fun avsnitt(ytelse: Ytelse): Array<String> {
        return avsnittBuilder(ytelse)
    }

    companion object {
        val STANDARD_BUNNTEKSTER = arrayOf(
            ÅRSAK_TIL_FEILUTBETALING,
            DETTE_LEGGES_VEKT_PÅ,
            FORELØPIG_VURDERING,
            HVORDAN_UTTALE_SEG,
            RETT_TIL_INNSYN,
            PERSONVÆRN_ERKLÆRING,
            SPØRSMÅL,
        )
    }
}
