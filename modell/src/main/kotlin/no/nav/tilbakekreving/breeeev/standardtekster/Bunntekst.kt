package no.nav.tilbakekreving.breeeev.standardtekster

import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import java.math.BigDecimal

enum class Bunntekst(
    val tittel: String,
    val avsnitt: Array<String>,
    private val gjelderIkkeYtelser: Array<Ytelse> = emptyArray(),
) {
    RENTER(
        tittel = "Renter",
        avsnitt = arrayOf("Hvis du forårsaker en feilutbetaling grovt uaktsomt eller forsettlig, skal du betale et rentetillegg på ti prosent av beløpet du skal betale tilbake. Dette står i folketrygdloven § 22‑17a. Vi har vurdert at dette er tilfelle i din sak, og at du derfor må betale et rentetillegg på ti prosent."),
    ),
    SKATT(
        tittel = "Skatt og skatteoppgjør",
        avsnitt = arrayOf("Nav gir opplysninger til Skatteetaten. Skatteetaten vil vurdere om det er grunnlag for å endre skatteoppgjør."),
        gjelderIkkeYtelser = arrayOf(
            Ytelse.Tiltakspenger,
            Ytelse.Tilleggsstønad,
        ),
    ),
    HVORDAN_BETALE_TILBAKE(
        tittel = "Hvordan betaler du tilbake?",
        avsnitt = arrayOf(
            "Du vil få faktura fra Skatteetaten på det beløpet du skal betale tilbake. På fakturaen vil det stå informasjon om nøyaktig beløp, kontonummer og forfallsdato. Du trenger ikke å gjøre noe før du får fakturaen.",
            "Du finner mer informasjon på skatteetaten.no/betale.",
        ),
    ),
    RETT_TIL_Å_KLAGE(
        tittel = "Du har rett til å klage",
        avsnitt = arrayOf(
            "Hvis du mener vedtaket er feil, kan du klage innen 6 uker fra den datoen vedtaket har kommet fram til deg. Nav klageinstans er klageinstans dersom du klager.",
            "Du finner skjema og informasjon på nav.no/klage. Nav kan veilede deg på telefon om hvordan du sender en klage. Nav-kontoret ditt kan også hjelpe deg med å skrive en klage. Kontakt oss på telefon 55 55 33 33 hvis du trenger hjelp.",
            "Du må klage på vedtaket for å kunne anke saken til Trygderetten og ta ut søksmål for domstolene. Dette følger av folketrygdloven § 21-12.",
            "Du kan be om å få utsatt tilbakebetalingen frem til klagen din er behandlet. Dette følger av forvaltningsloven § 42. Du må som regel begynne å betale tilbake beløpet når du får fakturaen, selv om du klager på dette vedtaket. Hvis du får medhold i klagen din, betaler vi tilbake de pengene du allerede har betalt.",
            "Hvis du får medhold i klagen, kan du få dekket vesentlige utgifter som har vært nødvendige for å få endret vedtaket, for eksempel hjelp fra advokat.",
            "Du kan ha krav på fri rettshjelp etter rettshjelploven. Du kan få mer informasjon om denne ordningen hos advokater, statsforvalteren eller Nav.",
            "Du kan lese om saksomkostninger i forvaltningsloven § 36.",
            "Hvis du sender klage i posten, må du signere klagen.",
            "Mer informasjon om klagerettigheter finner du på http://nav.no/klagerettigheter.",
        ),
    ),
    RETT_TIL_INNSYN(
        tittel = "Du har rett til innsyn",
        avsnitt = arrayOf(
            "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.",
        ),
    ),
    PERSONVÆRN_ERKLÆRING(
        tittel = "Du har rettigheter knyttet til personopplysningene dine",
        avsnitt = arrayOf("Du finner informasjon om hvordan Nav behandler personopplysningene dine, og hvilke rettigheter du har, på nav.no/personvernerklaering. "),
    ),
    RETT_TIL_HJELP(
        tittel = "Du har rett til å få hjelp fra andre",
        avsnitt = arrayOf("Du kan be om hjelp fra andre under hele saksbehandlingen, for eksempel fra en advokat, rettshjelper, en organisasjon du er medlem av, eller en myndig person over 18 år. Dette følger av forvaltningsloven § 12. Hvis den som hjelper deg ikke er advokat, må du gi denne personen skriftlig fullmakt. Bruk skjemaet du finner på nav.no/fullmakt."),
    ),
    SPØRSMÅL(
        tittel = "Har du spørsmål?",
        avsnitt = arrayOf(
            "Du finner mer informasjon på nav.no",
            "På nav.no/kontakt kan du chatte eller skrive til oss.",
            "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09.00-15.00.",
        ),
    ),
    ;

    fun gjelder(ytelse: Ytelse): Boolean {
        return ytelse !in gjelderIkkeYtelser
    }

    companion object {
        fun finnTekster(
            vedtakOppsummering: Beregningsresultat,
            ytelse: Ytelse,
        ): Set<Bunntekst> {
            return buildSet {
                if (vedtakOppsummering.vedtaksresultat != Vedtaksresultat.INGEN_TILBAKEBETALING) {
                    if (vedtakOppsummering.totaltRentebeløp > BigDecimal.ZERO) {
                        add(RENTER)
                    }
                    add(SKATT)
                    add(HVORDAN_BETALE_TILBAKE)
                    add(RETT_TIL_Å_KLAGE)
                }
                addAll(Bunntekst.STANDARD_BUNNTEKSTER)
            }
                .filter { it.gjelder(ytelse) }
                .toSet()
        }

        val STANDARD_BUNNTEKSTER = arrayOf(
            RETT_TIL_INNSYN,
            PERSONVÆRN_ERKLÆRING,
            RETT_TIL_HJELP,
            SPØRSMÅL,
        )
    }
}
