package no.nav.tilbakekreving.pdf.dokumentasjonsgenerator

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Brukes for å generere tekster for oppsummering av vedtaket i vedtaksbrevet. Resultatet er tekster med markup, som med
 * "Insert markup"-macroen kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
@Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorVedtaksoppsummering {
    companion object {
        private val JANUAR_15: LocalDate = LocalDate.of(2020, 1, 15)
        private val FEBRUAR_15: LocalDate = LocalDate.of(2020, 2, 15)
        private val tilbakekrevingsResultat =
            listOf(
                Vedtaksresultat.FULL_TILBAKEBETALING,
                Vedtaksresultat.DELVIS_TILBAKEBETALING,
                Vedtaksresultat.INGEN_TILBAKEBETALING,
            )
        private val trueFalse = booleanArrayOf(true, false)

        const val VEDTAK_START = "vedtak/vedtak_start"
    }

    @Test
    fun `list ut vedtak start for BA bokmål`() {
        val ytelseType = YtelsestypeDTO.BARNETRYGD
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenRenterUtenSkatt(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenRenterUtenSkatt(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for BA nynorsk`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.BARNETRYGD
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenRenterUtenSkatt(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenRenterUtenSkatt(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFBT bokmål`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.BARNETILSYN
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFBT nynorsk`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.BARNETILSYN
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFOG bokmål`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.OVERGANGSSTØNAD
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFOG nynorsk`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.OVERGANGSSTØNAD
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartAllePermutasjoner(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpAllePermutasjoner(ytelseType, språkkode, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFSP bokmål`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.SKOLEPENGER
        val nb: Språkkode = Språkkode.NB
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, nb, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, nb, resultatType)
        }
    }

    @Test
    fun `list ut vedtak start for EFSP nynorsk`() {
        val ytelseType: YtelsestypeDTO = YtelsestypeDTO.SKOLEPENGER
        val språkkode: Språkkode = Språkkode.NN
        for (resultatType in tilbakekrevingsResultat) {
            for (medVarsel in trueFalse) {
                listVedtakStartUtenSkatt(ytelseType, språkkode, resultatType, medVarsel)
            }
            listVedtakStartMedKorrigertBeløpUtenSkatt(ytelseType, språkkode, resultatType)
        }
    }

    private fun listVedtakStartAllePermutasjoner(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
        medVarsel: Boolean,
    ) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 100)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 100)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 0)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartUtenRenterUtenSkatt(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
        medVarsel: Boolean,
    ) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartUtenSkatt(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
        medVarsel: Boolean,
    ) {
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 10, 0)
        genererVedtakStart(ytelseType, nb, resultatType, medVarsel, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpAllePermutasjoner(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
    ) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 100)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 100)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 0)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpUtenRenterUtenSkatt(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
    ) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun listVedtakStartMedKorrigertBeløpUtenSkatt(
        ytelseType: YtelsestypeDTO,
        nb: Språkkode,
        resultatType: Vedtaksresultat,
    ) {
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 10, 0)
        genererVedtakStartMedKorrigertBeløp(ytelseType, nb, resultatType, 0, 0)
    }

    private fun genererVedtakStart(
        ytelseType: YtelsestypeDTO,
        språkkode: Språkkode,
        tilbakebetaling: Vedtaksresultat,
        medVarsel: Boolean,
        renter: Long,
        skatt: Long,
    ) {
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, medVarsel, renter, skatt, false, false, false)
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, medVarsel, renter, skatt, false, true, false)
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, medVarsel, renter, skatt, false, true, true)
    }

    private fun genererVedtakStartMedKorrigertBeløp(
        ytelseType: YtelsestypeDTO,
        språkkode: Språkkode,
        tilbakebetaling: Vedtaksresultat,
        renter: Long,
        skatt: Long,
    ) {
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, true, renter, skatt, true, false, false)
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, true, renter, skatt, true, true, false)
        genererVedtakStart(ytelseType, språkkode, tilbakebetaling, true, renter, skatt, true, true, true)
    }

    private fun genererVedtakStart(
        ytelseType: YtelsestypeDTO,
        språkkode: Språkkode,
        tilbakebetaling: Vedtaksresultat,
        medVarsel: Boolean,
        renter: Long,
        skatt: Long,
        medKorrigertBeløp: Boolean,
        erRevurdering: Boolean,
        erRevurderingEtterKlageNfp: Boolean,
    ) {
        val totalt = 1000L
        val totaltMedRenter = totalt + renter
        val resultat =
            HbTotalresultat(
                hovedresultat = tilbakebetaling,
                totaltTilbakekrevesBeløp = BigDecimal.valueOf(totalt),
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(totaltMedRenter),
                totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.valueOf(totaltMedRenter - skatt),
                totaltRentebeløp = BigDecimal.valueOf(renter),
            )
        val varsel =
            if (medKorrigertBeløp) {
                HbVarsel(
                    varsletBeløp = BigDecimal.valueOf(25000L),
                    varsletDato = JANUAR_15,
                )
            } else if (medVarsel) {
                HbVarsel(
                    varsletBeløp = BigDecimal.valueOf(1000L),
                    varsletDato = JANUAR_15,
                )
            } else {
                null
            }

        val behandling =
            HbBehandling(
                erRevurdering = erRevurdering,
                erRevurderingEtterKlageNfp = erRevurderingEtterKlageNfp,
                originalBehandlingsdatoFagsakvedtak = if (erRevurdering) FEBRUAR_15 else null,
            )

        val felles =
            HbVedtaksbrevFelles(
                brevmetadata = lagMetadata(ytelseType, språkkode),
                totalresultat = resultat,
                søker = HbPerson(navn = ""),
                konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                hjemmel = HbHjemmel(""),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                fagsaksvedtaksdato = JANUAR_15,
                varsel = varsel,
                erFeilutbetaltBeløpKorrigertNed = medKorrigertBeløp,
                totaltFeilutbetaltBeløp = BigDecimal.valueOf(1000),
                behandling = behandling,
                harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
            )
        val vedtakStart: String = FellesTekstformaterer.lagDeltekst(felles, VEDTAK_START)
        prettyPrint(
            tilbakebetaling,
            medVarsel,
            renter,
            skatt,
            vedtakStart,
            medKorrigertBeløp,
            erRevurdering,
            erRevurderingEtterKlageNfp,
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

    private fun prettyPrint(
        tilbakebetaling: Vedtaksresultat,
        medVarsel: Boolean,
        renter: Long,
        skatt: Long,
        generertTekst: String,
        medKorrigertBeløp: Boolean,
        erRevurdering: Boolean,
        erRevurderingEtterKlageNfp: Boolean,
    ) {
        println(
            ("*[ " + tilbakebetaling.navn) + " - " +
                (if (medVarsel) "med varsel" else "uten varsel") + " - " +
                (if (skatt != 0L) "med skatt" else "uten skatt") + " - " +
                (if (renter != 0L) "med renter" else "uten renter") +
                (if (medKorrigertBeløp) " - med korrigert beløp" else "") +
                (if (erRevurdering) " - revurdering " else "") +
                (if (erRevurderingEtterKlageNfp) " klage nfp" else "") + " ]*",
        )
        val parametrisertTekst =
            generertTekst
                .replace(" 1\u00A0010\u00A0kroner".toRegex(), " <skyldig beløp> kroner")
                .replace(" 1\u00A0000\u00A0kroner".toRegex(), " <skyldig beløp> kroner")
                .replace(" 910\u00A0kroner".toRegex(), " <skyldig beløp uten skatt> kroner")
                .replace(" 900\u00A0kroner".toRegex(), " <skyldig beløp uten skatt> kroner")
                .replace(" 25\u00A0000\u00A0kroner".toRegex(), " <varslet beløp> kroner")
                .replace("15. januar 2020", if (medVarsel) "<varseldato>" else "<vedtaksdato>")
                .replace("15. februar 2020", "<original vedtaksdato>")
        println(parametrisertTekst)
        println()
    }
}
