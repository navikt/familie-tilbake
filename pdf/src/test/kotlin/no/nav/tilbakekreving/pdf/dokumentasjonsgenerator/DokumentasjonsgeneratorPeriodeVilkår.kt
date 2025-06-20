package no.nav.tilbakekreving.pdf.dokumentasjonsgenerator

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.AvsnittUtil
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
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere vilkårtekster for perioder. Resultatet er tekster med markup, som med "Insert markup"-macroen
 * kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
class DokumentasjonsgeneratorPeriodeVilkår {
    @Test
    fun `generer vilkår for BA bokmål`() {
        lagVilkårstekster(YtelsestypeDTO.BARNETRYGD, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for BA nynorsk`() {
        lagVilkårstekster(YtelsestypeDTO.BARNETRYGD, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFOG bokmål`() {
        lagVilkårstekster(YtelsestypeDTO.OVERGANGSSTØNAD, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFOG nynorsk`() {
        lagVilkårstekster(YtelsestypeDTO.OVERGANGSSTØNAD, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFBT bokmål`() {
        lagVilkårstekster(YtelsestypeDTO.BARNETILSYN, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFBT nynorsk`() {
        lagVilkårstekster(YtelsestypeDTO.BARNETILSYN, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFSP bokmål`() {
        lagVilkårstekster(YtelsestypeDTO.SKOLEPENGER, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFSP nynorsk`() {
        lagVilkårstekster(YtelsestypeDTO.SKOLEPENGER, Språkkode.NN)
    }

    private fun lagVilkårstekster(
        ytelsetype: YtelsestypeDTO,
        språkkode: Språkkode,
    ) {
        vilkårResultat.forEach { resultat ->
            aktsomheter.forEach { vurdering ->
                foreldelseVurderinger.forEach { foreldelseVurdering ->
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        resultat,
                        vurdering,
                        foreldelseVurdering,
                        fritekst = false,
                        pengerIBehold = false,
                        lavtBeløp = false,
                    )
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        resultat,
                        vurdering,
                        foreldelseVurdering,
                        fritekst = true,
                        pengerIBehold = false,
                        lavtBeløp = false,
                    )
                    if (vurdering === Aktsomhet.SIMPEL_UAKTSOMHET) {
                        lagResultatOgVurderingTekster(
                            ytelsetype,
                            språkkode,
                            resultat,
                            vurdering,
                            foreldelseVurdering,
                            fritekst = false,
                            pengerIBehold = false,
                            lavtBeløp = true,
                        )
                    }
                }
            }
        }
        foreldelseVurderinger.forEach { foreldelseVurdering ->
            trueFalse.forEach { fritekst: Boolean ->
                trueFalse.forEach { pengerIBehold ->
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        Vilkårsvurderingsresultat.GOD_TRO,
                        AnnenVurdering.GOD_TRO,
                        foreldelseVurdering,
                        fritekst,
                        pengerIBehold,
                        lavtBeløp = false,
                    )
                }
            }
        }
        lagResultatOgVurderingTekster(
            ytelsetype,
            språkkode,
            Vilkårsvurderingsresultat.UDEFINERT,
            AnnenVurdering.FORELDET,
            Foreldelsesvurderingstype.FORELDET,
            fritekst = false,
            pengerIBehold = false,
            lavtBeløp = false,
        )
        lagResultatOgVurderingTekster(
            ytelsetype,
            språkkode,
            Vilkårsvurderingsresultat.UDEFINERT,
            AnnenVurdering.FORELDET,
            Foreldelsesvurderingstype.FORELDET,
            fritekst = true,
            pengerIBehold = false,
            lavtBeløp = false,
        )
    }

    private fun lagResultatOgVurderingTekster(
        ytelsetype: YtelsestypeDTO,
        språkkode: Språkkode,
        resultat: Vilkårsvurderingsresultat,
        vurdering: Vurdering,
        foreldelsevurdering: Foreldelsesvurderingstype,
        fritekst: Boolean,
        pengerIBehold: Boolean,
        lavtBeløp: Boolean,
    ) {
        val periodeOgFelles =
            lagPeriodeOgFelles(
                ytelsetype,
                språkkode,
                resultat,
                vurdering,
                lavtBeløp,
                foreldelsevurdering,
                fritekst,
                pengerIBehold,
            )
        val vilkårTekst = lagVilkårTekst(periodeOgFelles)
        val overskrift = overskrift(resultat, vurdering, lavtBeløp, fritekst, pengerIBehold, foreldelsevurdering)
        val prettyprint = prettyprint(vilkårTekst, overskrift)
        println()
        println(prettyprint)
    }

    private fun lagVilkårTekst(periodeOgFelles: HbVedtaksbrevPeriodeOgFelles): String {
        if (periodeOgFelles.periode.vurderinger.harForeldelsesavsnitt) {
            return FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_FORELDELSE) +
                System.lineSeparator() + System.lineSeparator() +
                FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_VILKÅR)
        }
        return FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_VILKÅR)
    }

    private fun lagPeriodeOgFelles(
        ytelsetype: YtelsestypeDTO,
        språkkode: Språkkode,
        vilkårResultat: Vilkårsvurderingsresultat?,
        vurdering: Vurdering,
        lavtBeløp: Boolean,
        foreldelsevurdering: Foreldelsesvurderingstype,
        fritekst: Boolean,
        pengerIBehold: Boolean,
    ): HbVedtaksbrevPeriodeOgFelles {
        val fellesBuilder = lagFelles(ytelsetype, språkkode)

        val vurderinger =
            HbVurderinger(
                foreldelsevurdering = foreldelsevurdering,
                aktsomhetsresultat = vurdering,
                unntasInnkrevingPgaLavtBeløp = lavtBeløp,
                fritekst = if (fritekst) "[ fritekst her ]" else null,
                vilkårsvurderingsresultat = vilkårResultat,
                beløpIBehold =
                    if (AnnenVurdering.GOD_TRO === vurdering) {
                        if (pengerIBehold) BigDecimal.valueOf(3999) else BigDecimal.ZERO
                    } else {
                        null
                    },
                foreldelsesfrist =
                    if (foreldelsevurdering in
                        setOf(
                            Foreldelsesvurderingstype.FORELDET,
                            Foreldelsesvurderingstype.TILLEGGSFRIST,
                        )
                    ) {
                        FORELDELSESFRIST
                    } else {
                        null
                    },
                fritekstForeldelse =
                    if (foreldelsevurdering in
                        setOf(
                            Foreldelsesvurderingstype.FORELDET,
                            Foreldelsesvurderingstype.TILLEGGSFRIST,
                        ) &&
                        fritekst
                    ) {
                        "[ fritekst her ]"
                    } else {
                        null
                    },
                oppdagelsesdato =
                    if (Foreldelsesvurderingstype.TILLEGGSFRIST == foreldelsevurdering) {
                        OPPDAGELSES_DATO
                    } else {
                        null
                    },
            )

        val periodeBuilder =
            HbVedtaksbrevsperiode(
                periode = JANUAR,
                kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal.ZERO),
                fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                vurderinger = vurderinger,
                resultat =
                    HbResultat(
                        tilbakekrevesBeløp = BigDecimal.valueOf(9999),
                        rentebeløp = BigDecimal.ZERO,
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(9999),
                        foreldetBeløp = BigDecimal.valueOf(2999),
                    ),
                førstePeriode = true,
            )
        return HbVedtaksbrevPeriodeOgFelles(fellesBuilder, periodeBuilder)
    }

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
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(6855),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 4),
            totalresultat = HbTotalresultat(
                hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                totaltRentebeløp = BigDecimal.ZERO,
            ),
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

    private fun overskrift(
        resultat: Vilkårsvurderingsresultat,
        vurdering: Vurdering?,
        lavtBeløp: Boolean,
        fritekst: Boolean,
        pengerIBehold: Boolean,
        foreldelsevurdering: Foreldelsesvurderingstype,
    ): String =
        (
            "*[ ${hentVilkårresultatOverskriftDel(resultat)}" +
                (if (vurdering != null) " - " + vurdering.navn else "") +
                (if (fritekst) " - med fritekst" else " - uten fritekst") +
                hentVIlkårsvurderingOverskriftDel(foreldelsevurdering) +
                (if (pengerIBehold) " - penger i behold" else "") +
                (if (lavtBeløp) " - lavt beløp" else "") +
                " ]*"
        )

    private fun prettyprint(
        vilkårTekst: String,
        overskrift: String,
    ): String =
        vilkårTekst
            .replace("__.+".toRegex(), overskrift)
            .replace(" 4\u00A0321\u00A0kroner", " <4 rettsgebyr> kroner")
            .replace(" 2\u00A0999\u00A0kroner", " <foreldet beløp> kroner")
            .replace(" 3\u00A0999\u00A0kroner", " <beløp i behold> kroner")
            .replace("1. januar 2019", "<periode start>")
            .replace("31. januar 2019", "<periode slutt>")
            .replace("1. mars 2019", "<oppdagelsesdato>")
            .replace("1. desember 2019", "<foreldelsesfrist>")

    companion object {
        private val vilkårResultat =
            arrayOf(
                Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )
        private val foreldelseVurderinger =
            arrayOf(
                Foreldelsesvurderingstype.IKKE_VURDERT,
                Foreldelsesvurderingstype.IKKE_FORELDET,
                Foreldelsesvurderingstype.TILLEGGSFRIST,
            )
        private val aktsomheter =
            arrayOf(
                Aktsomhet.SIMPEL_UAKTSOMHET,
                Aktsomhet.GROV_UAKTSOMHET,
                Aktsomhet.FORSETT,
            )
        private val trueFalse = booleanArrayOf(true, false)
        private val JANUAR = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        private val FORELDELSESFRIST = LocalDate.of(2019, 12, 1)
        private val OPPDAGELSES_DATO = LocalDate.of(2019, 3, 1)
    }

    private fun hentVilkårresultatOverskriftDel(resultat: Vilkårsvurderingsresultat): String =
        when (resultat) {
            Vilkårsvurderingsresultat.UDEFINERT -> "Foreldelse"
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT -> "Forsto/Burde forstått"
            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER -> "Feilaktive opplysninger"
            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER -> "Mangelfull opplysninger"
            Vilkårsvurderingsresultat.GOD_TRO -> "God tro"
            else -> throw IllegalArgumentException("Vilkårsvurderingsresultat ikke støttet. Resultat: $resultat")
        }

    private fun hentVIlkårsvurderingOverskriftDel(foreldelsevurdering: Foreldelsesvurderingstype): String =
        when (foreldelsevurdering) {
            Foreldelsesvurderingstype.IKKE_VURDERT -> " - automatisk vurdert"
            Foreldelsesvurderingstype.IKKE_FORELDET -> " - ikke foreldet"
            Foreldelsesvurderingstype.FORELDET -> " - foreldet"
            Foreldelsesvurderingstype.TILLEGGSFRIST -> " - med tilleggsfrist"
            else -> throw IllegalArgumentException("Foreldelsesvurderingstype ikke støttet. Type: $foreldelsevurdering")
        }
}
