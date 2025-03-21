package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevDatoer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløp
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsestypePerYtelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.TreeMap

class TekstformatererVedtaksbrevAllePermutasjonerAvFaktaTest {
    private val januar =
        Datoperiode(
            LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 1, 31),
        )

    private val brevmetadata =
        Brevmetadata(
            sakspartId = "123456",
            sakspartsnavn = "Test",
            mottageradresse = Adresseinfo("ident", "bob"),
            behandlendeEnhetsNavn = "Nav Familie- og pensjonsytelser Skien",
            ansvarligSaksbehandler = "Bob",
            saksnummer = "1232456",
            språkkode = Språkkode.NB,
            ytelsestype = Ytelsestype.BARNETRYGD,
            gjelderDødsfall = false,
        )

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFOG`() {
        lagTeksterOgValider(
            Ytelsestype.OVERGANGSSTØNAD,
            Språkkode.NB,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFOG nynorsk`() {
        lagTeksterOgValider(
            Ytelsestype.OVERGANGSSTØNAD,
            Språkkode.NN,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFBT`() {
        lagTeksterOgValider(
            Ytelsestype.BARNETILSYN,
            Språkkode.NB,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFBT nynorsk`() {
        lagTeksterOgValider(
            Ytelsestype.BARNETILSYN,
            Språkkode.NN,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFSP`() {
        lagTeksterOgValider(
            Ytelsestype.SKOLEPENGER,
            Språkkode.NB,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for EFSP nynorsk`() {
        lagTeksterOgValider(
            Ytelsestype.SKOLEPENGER,
            Språkkode.NN,
            HendelseMedUndertype(Hendelsestype.STØNADSPERIODE, Hendelsesundertype.UTVIDELSE_UTDANNING),
        )
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for BA`() {
        lagTeksterOgValider(Ytelsestype.BARNETRYGD, Språkkode.NB)
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for BA nynorsk`() {
        lagTeksterOgValider(Ytelsestype.BARNETRYGD, Språkkode.NN)
    }

    @SafeVarargs
    private fun lagTeksterOgValider(
        ytelsestype: Ytelsestype,
        språkkode: Språkkode,
        vararg unntak: HendelseMedUndertype,
    ) {
        val felles: HbVedtaksbrevFelles = lagFellesBuilder(språkkode, ytelsestype)

        val resultat = lagFaktatekster(felles, ytelsestype)
        sjekkVerdier(resultat, *unntak)
    }

    private fun sjekkVerdier(
        verdier: Map<HendelseMedUndertype, String>,
        vararg unntattUnikhet: HendelseMedUndertype,
    ) {
        val tekstTilHendelsestyper = TreeMap<String, MutableSet<HendelseMedUndertype>>()
        verdier
            .filter { (key, _) -> key !in unntattUnikhet }
            .forEach { (key, value) ->
                if (tekstTilHendelsestyper.containsKey(value)) {
                    tekstTilHendelsestyper[value]!!.add(key)
                } else {
                    val liste: MutableSet<HendelseMedUndertype> = HashSet()
                    liste.add(key)
                    tekstTilHendelsestyper[value] = liste
                }
            }
        val feilmelding =
            tekstTilHendelsestyper
                .filter { (_, value) -> value.size > 1 }
                .map { (key, value) ->
                    """$value mapper alle til "$key"""
                }.joinToString("\n")

        if (feilmelding.isNotEmpty()) {
            throw AssertionError(feilmelding)
        }
    }

    private fun lagFaktatekster(
        felles: HbVedtaksbrevFelles,
        ytelsestype: Ytelsestype,
    ): Map<HendelseMedUndertype, String> {
        val resultat: MutableMap<HendelseMedUndertype, String> = LinkedHashMap()
        for (undertype in getFeilutbetalingsårsaker(ytelsestype)) {
            val periode: HbVedtaksbrevsperiode = lagPeriodeBuilder(HbFakta(undertype.hendelsestype, undertype.hendelsesundertype))
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)
            val tekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)
            resultat[undertype] = tekst
        }
        return resultat
    }

    private fun lagPeriodeBuilder(fakta: HbFakta): HbVedtaksbrevsperiode =
        HbVedtaksbrevsperiode(
            periode = januar,
            kravgrunnlag =
                HbKravgrunnlag(
                    feilutbetaltBeløp = BigDecimal.valueOf(10000),
                    utbetaltBeløp = BigDecimal.valueOf(33333),
                    riktigBeløp = BigDecimal.valueOf(23333),
                ),
            fakta = fakta,
            grunnbeløp = HbGrunnbeløp(null, "Seks ganger grunnbeløpet er 741 000 for perioden fra 01.05.2022"),
            vurderinger =
                HbVurderinger(
                    foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                    aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                    beløpIBehold = BigDecimal.valueOf(10000),
                ),
            resultat =
                HbResultat(
                    tilbakekrevesBeløp = BigDecimal(10000),
                    rentebeløp = BigDecimal(1000),
                    tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(9000),
                ),
            førstePeriode = true,
        )

    private fun lagFellesBuilder(
        språkkode: Språkkode,
        ytelsestype: Ytelsestype,
    ) = HbVedtaksbrevFelles(
        brevmetadata = brevmetadata.copy(språkkode = språkkode, ytelsestype = ytelsestype),
        hjemmel = HbHjemmel("Folketrygdloven"),
        totalresultat =
            HbTotalresultat(
                hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                totaltRentebeløp = BigDecimal.valueOf(1000),
                totaltTilbakekrevesBeløp = BigDecimal.valueOf(10000),
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(11000),
                totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                    BigDecimal.valueOf(11000),
            ),
        varsel =
            HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
        konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
        søker =
            HbPerson(
                navn = "Søker Søkersen",
            ),
        fagsaksvedtaksdato = LocalDate.now(),
        behandling = HbBehandling(),
        totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
        vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
        ansvarligBeslutter = "ansvarlig person sin signatur",
        datoer =
            HbVedtaksbrevDatoer(
                opphørsdatoDødSøker = LocalDate.of(2021, 5, 4),
                opphørsdatoDødtBarn = LocalDate.of(2021, 5, 4),
            ),
        harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
    )

    private fun getFeilutbetalingsårsaker(ytelsestype: Ytelsestype): List<HendelseMedUndertype> =
        HendelsestypePerYtelsestype
            .getHendelsestyper(ytelsestype)
            .map { hendelsestype ->
                HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).map { hendelsesundertype ->
                    HendelseMedUndertype(hendelsestype, hendelsesundertype)
                }
            }.flatten()
}
