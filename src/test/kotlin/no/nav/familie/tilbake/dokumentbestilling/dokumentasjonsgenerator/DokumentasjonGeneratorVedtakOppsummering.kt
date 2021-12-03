package no.nav.familie.tilbake.dokumentbestilling.dokumentasjonsgenerator

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Brukes for å generere tekster for oppsummering av vedtaket i vedtaksbrevet. Resultatet er tekster med markup, som med
 * "Insert markup"-macroen kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * FP/SVP/ES: https://confluence.adeo.no/display/TVF/Generert+dokumentasjon
 * SKOLEPENGER: https://confluence.adeo.no/display/MODNAV/Generert+dokumentasjon
 */
@Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonGeneratorVedtakOppsummering {

    companion object {

        private val JANUAR_15: LocalDate = LocalDate.of(2020, 1, 15)
        private val tilbakekrevingsResultat = listOf(Vedtaksresultat.FULL_TILBAKEBETALING,
                                                     Vedtaksresultat.DELVIS_TILBAKEBETALING,
                                                     Vedtaksresultat.INGEN_TILBAKEBETALING)
        private val trueFalse = booleanArrayOf(true, false)

        const val VEDTAK_START = "vedtak/vedtak_start"
    }

    @Test
    fun `list ut vedtak start for fp`() {
        val ytelseType = Ytelsestype.BARNETRYGD
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for fp nynorsk`() {
        val ytelseType: Ytelsestype = Ytelsestype.BARNETRYGD
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for es`() {
        val ytelseType: Ytelsestype = Ytelsestype.BARNETILSYN
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for es nynorsk`() {
        val ytelseType: Ytelsestype = Ytelsestype.BARNETILSYN
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for svp`() {
        val ytelseType: Ytelsestype = Ytelsestype.OVERGANGSSTØNAD
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for svp nynorsk`() {
        val ytelseType: Ytelsestype = Ytelsestype.OVERGANGSSTØNAD
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for frisinn`() {
        val ytelseType: Ytelsestype = Ytelsestype.SKOLEPENGER
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenRenter(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenRenter(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for frisinn nynorsk`() {
        val ytelseType: Ytelsestype = Ytelsestype.SKOLEPENGER
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenRenter(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenRenter(ytelseType, språkkode, resultatType)
        }
    }

    private fun listVedtakStartAllePermutasjoner(ytelseType: Ytelsestype,
                                                 nb: Språkkode,
                                                 resultatType: Vedtaksresultat,
                                                 medVarsel: Boolean) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 100)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 100)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 0)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartUtenRenter(ytelseType: Ytelsestype,
                                          nb: Språkkode,
                                          resultatType: Vedtaksresultat,
                                          medVarsel: Boolean) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 100)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartUtenSkatt(ytelseType: Ytelsestype,
                                         nb: Språkkode,
                                         resultatType: Vedtaksresultat,
                                         medVarsel: Boolean) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 0)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType: Ytelsestype,
                                                                  nb: Språkkode,
                                                                  resultatType: Vedtaksresultat) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 100)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 100)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 0)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpUtenRenter(ytelseType: Ytelsestype,
                                                           nb: Språkkode,
                                                           resultatType: Vedtaksresultat) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 100)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType: Ytelsestype,
                                                          nb: Språkkode,
                                                          resultatType: Vedtaksresultat) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 0)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun genererVedtakStart(ytelseType: Ytelsestype,
                                   språkkode: Språkkode,
                                   tilbakebetaling: Vedtaksresultat,
                                   medVarsel: Boolean,
                                   renter: Long,
                                   skatt: Long) {
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, medVarsel, renter, skatt, false)
    }

    private fun genererVedtakStartMedKorrigertBeløp(ytelseType: Ytelsestype,
                                                    språkkode: Språkkode,
                                                    tilbakebetaling: Vedtaksresultat,
                                                    renter: Long,
                                                    skatt: Long) {
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, true, renter, skatt, true)
    }

    private fun genererVedtakStart(ytelseType: Ytelsestype,
                                   språkkode: Språkkode,
                                   tilbakebetaling: Vedtaksresultat,
                                   medVarsel: Boolean,
                                   renter: Long,
                                   skatt: Long,
                                   medKorrigertBeløp: Boolean) {
        val totalt = 1000L
        val totaltMedRenter = totalt + renter
        val resultat = HbTotalresultat(hovedresultat = tilbakebetaling,
                                       totaltTilbakekrevesBeløp = BigDecimal.valueOf(totalt),
                                       totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(totaltMedRenter),
                                       totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.valueOf(totaltMedRenter - skatt),
                                       totaltRentebeløp = BigDecimal.valueOf(renter))
        val varsel = if (medKorrigertBeløp) {
            HbVarsel(varsletBeløp = BigDecimal.valueOf(25000L),
                     varsletDato = JANUAR_15)
        } else if (medVarsel) {
            HbVarsel(varsletBeløp = BigDecimal.valueOf(1000L),
                     varsletDato = JANUAR_15)
        } else null

        val felles = HbVedtaksbrevFelles(brevmetadata = lagMetadata(ytelseType, språkkode),
                                         totalresultat = resultat,
                                         søker = HbPerson(navn = ""),
                                         konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                                         hjemmel = HbHjemmel(""),
                                         vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                                         fagsaksvedtaksdato = JANUAR_15,
                                         varsel = varsel,
                                         erFeilutbetaltBeløpKorrigertNed = medKorrigertBeløp,
                                         totaltFeilutbetaltBeløp = BigDecimal.valueOf(1000),
                                         behandling = HbBehandling())
        val vedtakStart: String = FellesTekstformaterer.lagDeltekst(felles, VEDTAK_START)
        prettyPrint(tilbakebetaling, medVarsel, renter, skatt, vedtakStart, medKorrigertBeløp)
    }


    private fun lagMetadata(ytelsestype: Ytelsestype,
                            språkkode: Språkkode): Brevmetadata {

        return Brevmetadata(sakspartId = "",
                            sakspartsnavn = "",
                            mottageradresse = Adresseinfo("01020312345", "Bob"),
                            behandlendeEnhetsNavn = "Oslo",
                            ansvarligSaksbehandler = "Bob",
                            språkkode = språkkode,
                            ytelsestype = ytelsestype)
    }


    private fun prettyPrint(tilbakebetaling: Vedtaksresultat,
                            medVarsel: Boolean,
                            renter: Long,
                            skatt: Long,
                            generertTekst: String,
                            medKorrigertBeløp: Boolean) {
        println(("*[ " + tilbakebetaling.navn) + " - " +
                (if (medVarsel) "med varsel" else "uten varsel") + " - "
                + (if (skatt != 0L) "med skatt" else "uten skatt") + " - "
                + (if (renter != 0L) "med renter" else "uten renter")
                + (if (medKorrigertBeløp) " - med korrigert beløp" else "") + " ]*")
        val parametrisertTekst = generertTekst
                .replace(" 1\u00A0010\u00A0kroner".toRegex(), " <skyldig beløp> kroner")
                .replace(" 1\u00A0000\u00A0kroner".toRegex(), " <skyldig beløp> kroner")
                .replace(" 910\u00A0kroner".toRegex(), " <skyldig beløp uten skatt> kroner")
                .replace(" 900\u00A0kroner".toRegex(), " <skyldig beløp uten skatt> kroner")
                .replace(" 25\u00A0000\u00A0kroner".toRegex(), " <varslet beløp> kroner")
                .replace("15. januar 2020".toRegex(), if (medVarsel) "<varseldato>" else "<vedtaksdato>")
                .replace("\\[".toRegex(), "[ ")
                .replace("]".toRegex(), " ]")
        println(parametrisertTekst)
        println()
    }
}