package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsestypePerYtelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.TreeMap

class TekstformatererVedtaksbrevAllePermutasjonerAvFaktaTest {

    private val januar = Handlebarsperiode(LocalDate.of(2019, 1, 1),
                                           LocalDate.of(2019, 1, 31))

    private val brevmetadata = Brevmetadata(sakspartId = "123456",
                                            sakspartsnavn = "Test",
                                            mottageradresse = Adresseinfo("bob", "ident"),
                                            språkkode = Språkkode.NB,
                                            ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                            behandlendeEnhetsNavn = "Skien",
                                            ansvarligSaksbehandler = "Bob")

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for OS`() {
        lagTeksterOgValider(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NB)
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for OS nynorsk`() {
        lagTeksterOgValider(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NN)
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for BT`() {
        val unntak1 = setOf(HendelseMedUndertype(Hendelsestype.EF_ANNET,
                                                 Hendelsesundertype.MOTTAKER_DØD),
                            HendelseMedUndertype(Hendelsestype.EF_ANNET,
                                                 Hendelsesundertype.IKKE_OMSORG))
        lagTeksterOgValider(Ytelsestype.BARNETRYGD, Språkkode.NB, unntak1)
    }

    @Test
    fun `lagDeltekst skal støtte alle permutasjoner av fakta for BT nynorsk`() {
        val unntak1 = setOf(HendelseMedUndertype(Hendelsestype.EF_ANNET,
                                                 Hendelsesundertype.MOTTAKER_DØD),
                            HendelseMedUndertype(Hendelsestype.EF_ANNET,
                                                 Hendelsesundertype.IKKE_OMSORG))
        lagTeksterOgValider(Ytelsestype.BARNETRYGD, Språkkode.NN, unntak1)
    }

    @SafeVarargs
    private fun lagTeksterOgValider(ytelsestype: Ytelsestype,
                                    språkkode: Språkkode,
                                    vararg unntak: Set<HendelseMedUndertype>) {
        val felles: HbVedtaksbrevFelles = lagFellesBuilder(språkkode, ytelsestype)

        val resultat = lagFaktatekster(felles, ytelsestype)
        sjekkVerdier(resultat, *unntak)
    }

    private fun sjekkVerdier(verdier: Map<HendelseMedUndertype, String>, vararg unntattUnikhet: Set<HendelseMedUndertype>) {
        val tekstTilHendelsestype: MutableMap<String, MutableSet<HendelseMedUndertype>> =
                TreeMap<String, MutableSet<HendelseMedUndertype>>()
        for ((key, value) in verdier) {
            if (tekstTilHendelsestype.containsKey(value)) {
                tekstTilHendelsestype[value]!!.add(key)
            } else {
                val liste: MutableSet<HendelseMedUndertype> = HashSet()
                liste.add(key)
                tekstTilHendelsestype[value] = liste
            }
        }
        val hendelsestypeTilTekst: MutableMap<Set<HendelseMedUndertype>, String> = HashMap()
        for ((key, value) in tekstTilHendelsestype) {
            hendelsestypeTilTekst[value] = key
        }
        for (unntak in unntattUnikhet) {
            hendelsestypeTilTekst.remove(unntak)
        }
        var feilmelding = ""
        for ((key, value) in hendelsestypeTilTekst) {
            if (key.size > 1) {
                feilmelding += """$value mapper alle til $key
"""
            }
        }
        if (feilmelding.isNotEmpty()) {
            throw AssertionError(feilmelding)
        }
    }

    private fun lagFaktatekster(felles: HbVedtaksbrevFelles, ytelsestype: Ytelsestype): Map<HendelseMedUndertype, String> {
        val resultat: MutableMap<HendelseMedUndertype, String> = LinkedHashMap()
        for (undertype in getFeilutbetalingsårsaker(ytelsestype)) {
            val periode: HbVedtaksbrevsperiode = lagPeriodeBuilder(HbFakta(undertype.hendelsestype, undertype.hendelsesundertype))
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)
            val tekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)
            resultat[undertype] = tekst
        }
        return resultat
    }

    private fun lagPeriodeBuilder(fakta: HbFakta): HbVedtaksbrevsperiode {
        return HbVedtaksbrevsperiode(periode = januar,
                                     kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal.valueOf(10000),
                                                                   utbetaltBeløp = BigDecimal.valueOf(33333),
                                                                   riktigBeløp = BigDecimal.valueOf(23333)),
                                     vurderinger = HbVurderinger(foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                                                                 aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                                                                 vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                                                                 beløpIBehold = BigDecimal.valueOf(10000)),
                                     resultat = HbResultat(tilbakekrevesBeløp = BigDecimal(10000),
                                                           rentebeløp = BigDecimal(1000),
                                                           tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(9000)),
                                     fakta = fakta)
    }

    private fun lagFellesBuilder(språkkode: Språkkode, ytelsestype: Ytelsestype) =
            HbVedtaksbrevFelles(brevmetadata = brevmetadata.copy(språkkode = språkkode, ytelsestype = ytelsestype),
                                hjemmel = HbHjemmel("Folketrygdloven"),
                                totalresultat = HbTotalresultat(hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                                                                totaltRentebeløp = BigDecimal.valueOf(1000),
                                                                totaltTilbakekrevesBeløp = BigDecimal.valueOf(10000),
                                                                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(11000),
                                                                totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                                                                BigDecimal.valueOf(11000)),
                                varsel = HbVarsel(varsletBeløp = BigDecimal.valueOf(10000),
                                                  varsletDato = LocalDate.now().minusDays(100)),
                                konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                                søker = HbPerson(navn = "Søker Søkersen",
                                                 dødsdato = LocalDate.of(2018, 3, 1)),
                                fagsaksvedtaksdato = LocalDate.now(),
                                behandling = HbBehandling(),
                                totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
                                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR)


    private fun getFeilutbetalingsårsaker(ytelsestype: Ytelsestype): List<HendelseMedUndertype> {
        return HendelsestypePerYtelsestype.getHendelsestyper(ytelsestype).map { hendelsestype ->
            HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).map { hendelsesundertype ->
                HendelseMedUndertype(hendelsestype, hendelsesundertype)
            }
        }.flatten()
    }
}
