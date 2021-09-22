package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultatTestBuilder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvsnittUtilTest {

    private val januar = Handlebarsperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
    private val februar = Handlebarsperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))

    private val brevmetadata = Brevmetadata(sakspartId = "123456",
                                            sakspartsnavn = "Test",
                                            mottageradresse = Adresseinfo("ident", "bob"),
                                            språkkode = Språkkode.NB,
                                            ytelsestype = Ytelsestype.BARNETRYGD,
                                            behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
                                            ansvarligSaksbehandler = "Bob")

    private val vedtaksbrevFelles = HbVedtaksbrevFelles(brevmetadata = brevmetadata,
                                                        konfigurasjon = HbKonfigurasjon(klagefristIUker = 4),
                                                        søker = HbPerson(navn = "Søker Søkersen",
                                                                         dødsdato = LocalDate.of(2018, 3, 1)),
                                                        fagsaksvedtaksdato = LocalDate.now(),
                                                        behandling = HbBehandling(erRevurdering = false),
                                                        totalresultat = HbTotalresultat(Vedtaksresultat.DELVIS_TILBAKEBETALING,
                                                                                        BigDecimal(23002),
                                                                                        BigDecimal(23002),
                                                                                        BigDecimal(23002),
                                                                                        BigDecimal.ZERO),
                                                        hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                                                        totaltFeilutbetaltBeløp = BigDecimal.valueOf(20000),
                                                        vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                                                        ansvarligBeslutter = "ansvarlig person sin signatur")

    @Test
    fun `lagVedtaksbrevDeltIAvsnitt skal generere brev delt i avsnitt og underavsnitt`() {
        val vedtaksbrevData = vedtaksbrevFelles.copy(fagsaksvedtaksdato = LocalDate.now(),
                                                     behandling = HbBehandling(erRevurdering = false),
                                                     totalresultat = HbTotalresultat(Vedtaksresultat.DELVIS_TILBAKEBETALING,
                                                                                     BigDecimal(23002),
                                                                                     BigDecimal(23002),
                                                                                     BigDecimal(23002),
                                                                                     BigDecimal.ZERO),
                                                     fritekstoppsummering = "Her finner du friteksten til oppsummeringen",
                                                     hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                                                     varsel = HbVarsel(varsletBeløp = BigDecimal(33001),
                                                                       varsletDato = LocalDate.of(2020, 4, 4)),
                                                     vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR)

        val perioder =
                listOf(HbVedtaksbrevsperiode(periode = januar,
                                             kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
                                             fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                                             vurderinger =
                                             HbVurderinger(foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                                                           vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                                                                   .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                                                           aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                                                           fritekst = "Du er heldig som slapp å betale alt!",
                                                           særligeGrunner =
                                                           HbSærligeGrunner(listOf(SærligGrunn.TID_FRA_UTBETALING,
                                                                                   SærligGrunn.STØRRELSE_BELØP,
                                                                                   SærligGrunn.ANNET),
                                                                            "Fritekst særlige grunner",
                                                                            "Fritekst særlige grunner annet")),
                                             resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(20002)),
                       HbVedtaksbrevsperiode(periode = februar,
                                             vurderinger =
                                             HbVurderinger(foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                                                           vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                                                                   .FORSTO_BURDE_FORSTÅTT,
                                                           aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                                                           særligeGrunner =
                                                           HbSærligeGrunner(listOf(SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL,
                                                                                   SærligGrunn.STØRRELSE_BELØP))),
                                             fakta = HbFakta(Hendelsestype.BOR_MED_SØKER, Hendelsesundertype.BOR_IKKE_MED_BARN),
                                             kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal(3000),
                                                                           riktigBeløp = BigDecimal(3000),
                                                                           utbetaltBeløp = BigDecimal(6000)),
                                             resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(3000)))
        val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

        val resultat = AvsnittUtil.lagVedtaksbrevDeltIAvsnitt(data, "Du må betale tilbake overgangsstønaden")

        assertThat(resultat).hasSize(4)
        assertThat(resultat[0].avsnittstype).isEqualTo(Avsnittstype.OPPSUMMERING)
        assertThat(resultat[0].underavsnittsliste).hasSize(1)
        assertThat(resultat[0].underavsnittsliste[0].fritekstTillatt).isTrue()
        assertThat(resultat[1].avsnittstype).isEqualTo(Avsnittstype.PERIODE)
        assertThat(resultat[1].underavsnittsliste).hasSize(6)
        assertThat(resultat[1].underavsnittsliste.filter { it.fritekstTillatt }.size).isEqualTo(4)
        assertThat(resultat[2].avsnittstype).isEqualTo(Avsnittstype.PERIODE)
        assertThat(resultat[2].underavsnittsliste).hasSize(5)
        assertThat(resultat[2].underavsnittsliste.filter { it.fritekstTillatt }.size).isEqualTo(3)
        assertThat(resultat[3].avsnittstype).isEqualTo(Avsnittstype.TILLEGGSINFORMASJON)
        assertThat(resultat[3].underavsnittsliste).hasSize(11)
        resultat[3].underavsnittsliste.forEach { assertThat(it.fritekstTillatt).isFalse() }
    }


    @Test
    fun `parseTekst skal parse tekst til avsnitt`() {
        val tekst = "_Hovedoverskrift i brevet\n\n" +
                    "Brødtekst første avsnitt\n\n" +
                    "Brødtekst andre avsnitt\n\n" +
                    "_underoverskrift\n\n" +
                    "Brødtekst tredje avsnitt\n\n" +
                    "_Avsluttende overskrift uten etterfølgende tekst\n" + Vedtaksbrevsfritekst.markerValgfriFritekst(null)

        val resultat = AvsnittUtil.parseTekst(tekst, Avsnitt(), null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift i brevet")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        assertThat(underavsnitt).hasSize(4)
        assertThat(underavsnitt[0].overskrift).isNull()
        assertThat(underavsnitt[0].brødtekst).isEqualTo("Brødtekst første avsnitt")
        assertThat(underavsnitt[0].fritekstTillatt).isFalse
        assertThat(underavsnitt[1].overskrift).isNull()
        assertThat(underavsnitt[1].brødtekst).isEqualTo("Brødtekst andre avsnitt")
        assertThat(underavsnitt[1].fritekstTillatt).isFalse
        assertThat(underavsnitt[2].overskrift).isEqualTo("underoverskrift")
        assertThat(underavsnitt[2].brødtekst).isEqualTo("Brødtekst tredje avsnitt")
        assertThat(underavsnitt[2].fritekstTillatt).isFalse
        assertThat(underavsnitt[3].overskrift).isEqualTo("Avsluttende overskrift uten etterfølgende tekst")
        assertThat(underavsnitt[3].brødtekst).isNull()
        assertThat(underavsnitt[3].fritekstTillatt).isTrue
    }

    @Test
    fun `parseTekst skal plassere fritekstfelt etter første avsnitt når det er valgt`() {
        val tekst = "_Hovedoverskrift i brevet\n\n" +
                    "Brødtekst første avsnitt\n" +
                    "${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}\n" +
                    "_underoverskrift\n\n" +
                    "Brødtekst andre avsnitt\n\n" +
                    "_Avsluttende overskrift uten etterfølgende tekst"

        val resultat = AvsnittUtil.parseTekst(tekst, Avsnitt(), null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift i brevet")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        assertThat(underavsnitt).hasSize(3)
        assertThat(underavsnitt[0].overskrift).isNull()
        assertThat(underavsnitt[0].brødtekst).isEqualTo("Brødtekst første avsnitt")
        assertThat(underavsnitt[0].fritekstTillatt).isTrue
        assertThat(underavsnitt[1].overskrift).isEqualTo("underoverskrift")
        assertThat(underavsnitt[1].brødtekst).isEqualTo("Brødtekst andre avsnitt")
        assertThat(underavsnitt[1].fritekstTillatt).isFalse
        assertThat(underavsnitt[2].overskrift).isEqualTo("Avsluttende overskrift uten etterfølgende tekst")
        assertThat(underavsnitt[2].brødtekst).isNull()
        assertThat(underavsnitt[2].fritekstTillatt).isFalse
    }

    @Test
    fun `parseTekst skal plassere fritekstfelt etter overskriften når det er valgt`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n" +
                    "${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}\n" +
                    "Brødtekst første avsnitt\n\n" +
                    "_underoverskrift 2\n\n" +
                    "Brødtekst andre avsnitt"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        assertThat(underavsnitt).hasSize(3)
        assertThat(underavsnitt[0].overskrift).isEqualTo("underoverskrift 1")
        assertThat(underavsnitt[0].brødtekst).isNull()
        assertThat(underavsnitt[0].fritekstTillatt).isTrue
        assertThat(underavsnitt[1].overskrift).isNull()
        assertThat(underavsnitt[1].brødtekst).isEqualTo("Brødtekst første avsnitt")
        assertThat(underavsnitt[1].fritekstTillatt).isFalse
        assertThat(underavsnitt[2].overskrift).isEqualTo("underoverskrift 2")
        assertThat(underavsnitt[2].brødtekst).isEqualTo("Brødtekst andre avsnitt")
        assertThat(underavsnitt[2].fritekstTillatt).isFalse
    }

    @Test
    fun `parseTekst skal parse fritekstfelt med eksisterende fritekst`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n${Vedtaksbrevsfritekst.markerValgfriFritekst("fritekst linje 1\nfritekst linje2")}"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        //assertThat(underavsnitt).hasSize(1);
        assertThat(underavsnitt[0].overskrift).isEqualTo("underoverskrift 1")
        assertThat(underavsnitt[0].brødtekst).isNull()
        assertThat(underavsnitt[0].fritekstTillatt).isTrue
        assertThat(underavsnitt[0].fritekst).isEqualTo("fritekst linje 1\nfritekst linje2")
    }

    @Test
    fun `parseTekst skal skille mellom påkrevet og valgfritt fritekstfelt`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n${Vedtaksbrevsfritekst.markerPåkrevetFritekst(null, null)}\n" +
                    "_underoverskrift 2\n${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        assertThat(underavsnitt).hasSize(2)
        assertThat(underavsnitt[0].overskrift).isEqualTo("underoverskrift 1")
        assertThat(underavsnitt[0].brødtekst).isNull()
        assertThat(underavsnitt[0].fritekstTillatt).isTrue
        assertThat(underavsnitt[0].fritekstPåkrevet).isTrue
        assertThat(underavsnitt[0].fritekst).isEqualTo("")
        assertThat(underavsnitt[1].overskrift).isEqualTo("underoverskrift 2")
        assertThat(underavsnitt[1].brødtekst).isNull()
        assertThat(underavsnitt[1].fritekstTillatt).isTrue
        assertThat(underavsnitt[1].fritekstPåkrevet).isFalse
        assertThat(underavsnitt[1].fritekst).isEqualTo("")
    }

    @Test
    fun `parseTekst skal utlede underavsnittstype fra fritekstmarkering slik at det er mulig å skille mellom særlige grunner`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n" +
                    Vedtaksbrevsfritekst.markerValgfriFritekst(null, Underavsnittstype.SÆRLIGE_GRUNNER) +
                    "\n_underoverskrift 2\n" +
                    "brødtekst ${Vedtaksbrevsfritekst.markerValgfriFritekst(null, Underavsnittstype.SÆRLIGE_GRUNNER_ANNET)}" +
                    "\n_underoverskrift 3"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        assertThat(resultat.overskrift).isEqualTo("Hovedoverskrift")
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        assertThat(underavsnitt).hasSize(3)
        assertThat(underavsnitt[0].underavsnittstype).isEqualTo(Underavsnittstype.SÆRLIGE_GRUNNER)
        assertThat(underavsnitt[1].underavsnittstype).isEqualTo(Underavsnittstype.SÆRLIGE_GRUNNER_ANNET)
        assertThat(underavsnitt[1].brødtekst).isEqualTo("brødtekst ")
        assertThat(underavsnitt[1].fritekstTillatt).isTrue
        assertThat(underavsnitt[2].underavsnittstype).isEqualTo(Underavsnittstype.SÆRLIGE_GRUNNER_ANNET)
        assertThat(underavsnitt[2].fritekstTillatt).isFalse
    }
}
