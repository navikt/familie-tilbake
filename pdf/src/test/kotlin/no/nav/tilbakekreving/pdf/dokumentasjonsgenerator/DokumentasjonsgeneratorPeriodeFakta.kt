package no.nav.tilbakekreving.pdf.dokumentasjonsgenerator

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.HendelsestypePerYtelsestype
import no.nav.tilbakekreving.pdf.HendelsesundertypePerHendelsestype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.AvsnittUtil
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.HendelseMedUndertype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevDatoer
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløp
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere faktatekster for perioder. Resultatet er tekster med markup, som med "Insert markup"-macroen
 * kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
@Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorPeriodeFakta {
    private val januar = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

    @Test
    fun `list ut permutasjoner for BA bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.BARNETRYGD, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for BA nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.BARNETRYGD, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFOG bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.OVERGANGSSTØNAD, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFOG nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.OVERGANGSSTØNAD, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFBT bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.BARNETILSYN, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFBT nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.BARNETILSYN, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFSP bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.SKOLEPENGER, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFSP nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(YtelsestypeDTO.SKOLEPENGER, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    private fun prettyPrint(resultat: Map<HendelseMedUndertype, String>) {
        resultat.forEach { (typer, generertTekst) ->
            println("*[ ${typer.hendelsestype.name} - ${typer.hendelsesundertype.name} ]*")
            val parametrisertTekst =
                generertTekst
                    .replace(" 10\u00A0000\u00A0kroner".toRegex(), " <feilutbetalt beløp> kroner")
                    .replace(" 33\u00A0333\u00A0kroner".toRegex(), " <utbetalt beløp> kroner")
                    .replace(" 23\u00A0333\u00A0kroner".toRegex(), " <riktig beløp> kroner")
                    .replace("Søker Søkersen".toRegex(), "<søkers navn>")
                    .replace("2. mars 2018".toRegex(), "<opphørsdato søker døde>")
                    .replace("3. mars 2018".toRegex(), "<opphørsdato barn døde>")
                    .replace("4. mars 2018".toRegex(), "<opphørsdato ikke omsorg>")
                    .replace("ektefellen".toRegex(), "<ektefellen/partneren/samboeren>")
            println(parametrisertTekst)
            println()
        }
    }

    private fun lagFaktatekster(felles: HbVedtaksbrevFelles): Map<HendelseMedUndertype, String> =
        getFeilutbetalingsårsaker(felles.brevmetadata.ytelsestype).associateWith {
            val periode: HbVedtaksbrevsperiode = lagPeriodeBuilder(it)
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)
            FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)
        }

    private fun lagPeriodeBuilder(undertype: HendelseMedUndertype): HbVedtaksbrevsperiode =
        HbVedtaksbrevsperiode(
            periode = januar,
            vurderinger =
                HbVurderinger(
                    foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                    aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                    beløpIBehold = BigDecimal.valueOf(5000),
                ),
            kravgrunnlag =
                HbKravgrunnlag(
                    feilutbetaltBeløp = BigDecimal.valueOf(10000),
                    riktigBeløp = BigDecimal.valueOf(23333),
                    utbetaltBeløp = BigDecimal.valueOf(33333),
                ),
            resultat =
                HbResultat(
                    tilbakekrevesBeløp = BigDecimal.valueOf(5000),
                    tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(4002),
                    rentebeløp = BigDecimal.ZERO,
                ),
            fakta = HbFakta(undertype.hendelsestype, undertype.hendelsesundertype),
            grunnbeløp = HbGrunnbeløp(BigDecimal.TEN, "120"),
            førstePeriode = true,
        )

    private fun lagFelles(
        ytelsestype: YtelsestypeDTO,
        språkkode: Språkkode,
    ): HbVedtaksbrevFelles {
        val datoer =
            HbVedtaksbrevDatoer(
                LocalDate.of(2018, 3, 2),
                LocalDate.of(2018, 3, 3),
                LocalDate.of(2018, 3, 4),
            )

        return HbVedtaksbrevFelles(
            brevmetadata = lagMetadata(ytelsestype, språkkode),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat =
                HbTotalresultat(
                    hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    totaltRentebeløp = BigDecimal.valueOf(1000),
                    totaltTilbakekrevesBeløp = BigDecimal.valueOf(10000),
                    totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(11000),
                    totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                        BigDecimal.valueOf(6855),
                ),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(6855),
            varsel =
                HbVarsel(
                    varsletBeløp = BigDecimal.valueOf(10000),
                    varsletDato = LocalDate.now().minusDays(100),
                ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker = HbPerson(navn = "Søker Søkersen"),
            datoer = datoer,
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
        )
    }

    private fun lagMetadata(
        ytelsestype: YtelsestypeDTO,
        språkkode: Språkkode,
    ): Brevmetadata =
        Brevmetadata(
            sakspartId = "",
            sakspartsnavn = "",
            mottageradresse = Adresseinfo("01020312345", "Bob"),
            behandlendeEnhetsNavn = "Oslo",
            ansvarligSaksbehandler = "Bob",
            saksnummer = "1232456",
            språkkode = språkkode,
            ytelsestype = ytelsestype,
            gjelderDødsfall = false,
        )

    private fun getFeilutbetalingsårsaker(ytelseType: YtelsestypeDTO): List<HendelseMedUndertype> {
        val hendelseTyper: Set<Hendelsestype> = HendelsestypePerYtelsestype.getHendelsestyper(ytelseType)
        val hendelseUndertypePrHendelsestype = HendelsesundertypePerHendelsestype.HIERARKI
        val resultat: List<HendelseMedUndertype> =
            hendelseTyper
                .map {
                    hendelseUndertypePrHendelsestype[it]?.map { undertype -> HendelseMedUndertype(it, undertype) } ?: listOf()
                }.flatten()
        return resultat
    }
}
