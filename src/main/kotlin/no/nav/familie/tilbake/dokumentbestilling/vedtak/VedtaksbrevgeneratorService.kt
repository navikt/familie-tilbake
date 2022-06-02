package no.nav.familie.tilbake.dokumentbestilling.vedtak

import com.github.jknack.handlebars.internal.text.WordUtils
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevDatoer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class VedtaksbrevgeneratorService(private val tilbakekrevingBeregningService: TilbakekrevingsberegningService,
                                  private val eksterneDataForBrevService: EksterneDataForBrevService) {

    fun genererVedtaksbrevForSending(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                     brevmottager: Brevmottager): Brevdata {

        val vedtaksbrevsdata = hentDataForVedtaksbrev(vedtaksbrevgrunnlag, brevmottager)
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata
        val data = Fritekstbrevsdata(TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
                                     TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata),
                                     vedtaksbrevsdata.metadata)
        val vedleggHtml = if (vedtaksbrevsdata.vedtaksbrevsdata.felles.harVedlegg) {
            TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
        } else ""
        return Brevdata(mottager = brevmottager,
                        metadata = data.brevmetadata,
                        overskrift = data.overskrift,
                        brevtekst = data.brevtekst,
                        vedleggHtml = vedleggHtml)
    }

    fun genererVedtaksbrevForForhåndsvisning(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                             dto: HentForhåndvisningVedtaksbrevPdfDto): Brevdata {

        val vedtaksbrevsdata = hentDataForVedtaksbrev(vedtaksbrevgrunnlag,
                                                      dto.oppsummeringstekst,
                                                      dto.perioderMedTekst,
                                                      vedtaksbrevgrunnlag.brevmottager)
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata

        val vedleggHtml = if (hbVedtaksbrevsdata.felles.harVedlegg) {
            TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
        } else ""

        return Brevdata(mottager = vedtaksbrevgrunnlag.brevmottager,
                        metadata = vedtaksbrevsdata.metadata,
                        overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
                        brevtekst = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata),
                        vedleggHtml = vedleggHtml)
    }

    fun genererVedtaksbrevsdata(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag): HbVedtaksbrevsdata {
        val vedtaksbrevsdata = hentDataForVedtaksbrev(vedtaksbrevgrunnlag, vedtaksbrevgrunnlag.brevmottager)
        return vedtaksbrevsdata.vedtaksbrevsdata
    }

    private fun hentDataForVedtaksbrev(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                       brevmottager: Brevmottager): Vedtaksbrevsdata {
        val fritekstoppsummering = vedtaksbrevgrunnlag.behandling.vedtaksbrevOppsummering?.oppsummeringFritekst
        val fritekstPerioder: List<PeriodeMedTekstDto> =
                VedtaksbrevFritekstMapper.mapFritekstFraDb(vedtaksbrevgrunnlag.behandling.eksisterendePerioderForBrev)
        return hentDataForVedtaksbrev(vedtaksbrevgrunnlag, fritekstoppsummering, fritekstPerioder, brevmottager)
    }

    private fun hentDataForVedtaksbrev(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                       oppsummeringFritekst: String?,
                                       perioderFritekst: List<PeriodeMedTekstDto>,
                                       brevmottager: Brevmottager): Vedtaksbrevsdata {
        val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(vedtaksbrevgrunnlag.bruker.ident,
                                                                           vedtaksbrevgrunnlag.fagsystem)
        val beregnetResultat = tilbakekrevingBeregningService.beregn(vedtaksbrevgrunnlag.behandling.id)
        val brevMetadata: Brevmetadata = lagMetadataForVedtaksbrev(vedtaksbrevgrunnlag,
                                                                   personinfo,
                                                                   beregnetResultat.vedtaksresultat,
                                                                   brevmottager)
        val data: HbVedtaksbrevsdata = lagHbVedtaksbrevsdata(vedtaksbrevgrunnlag,
                                                             personinfo,
                                                             beregnetResultat,
                                                             oppsummeringFritekst,
                                                             perioderFritekst,
                                                             brevMetadata)
        return Vedtaksbrevsdata(data, brevMetadata)
    }

    private fun lagHbVedtaksbrevsdata(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                      personinfo: Personinfo,
                                      beregningsresultat: Beregningsresultat,
                                      oppsummeringFritekst: String?,
                                      perioderFritekst: List<PeriodeMedTekstDto>,
                                      brevmetadata: Brevmetadata): HbVedtaksbrevsdata {
        val vedtaksbrevtype = vedtaksbrevgrunnlag.utledVedtaksbrevstype()
        val effektForBruker: VedtakHjemmel.EffektForBruker = utledEffektForBruker(vedtaksbrevgrunnlag, beregningsresultat)
        val klagebehandling = vedtaksbrevgrunnlag.klagebehandling
        val hbHjemmel = VedtakHjemmel.lagHjemmel(beregningsresultat.vedtaksresultat,
                                                 vedtaksbrevgrunnlag,
                                                 effektForBruker,
                                                 brevmetadata.språkkode,
                                                 visHjemmelForRenter = true,
                                                 klagebehandling) // sannsynligvis hjemmel
        val perioder: List<HbVedtaksbrevsperiode> = lagHbVedtaksbrevPerioder(vedtaksbrevgrunnlag,
                                                                             beregningsresultat,
                                                                             perioderFritekst)
        val hbTotalresultat: HbTotalresultat = lagHbTotalresultat(beregningsresultat.vedtaksresultat, beregningsresultat)
        val hbBehandling: HbBehandling = lagHbBehandling(vedtaksbrevgrunnlag)
        val varsletBeløp = vedtaksbrevgrunnlag.varsletBeløp
        val varsletDato = vedtaksbrevgrunnlag.sisteVarsel?.sporbar?.opprettetTid?.toLocalDate()
        val ansvarligBeslutter = if (vedtaksbrevgrunnlag.aktivtSteg in setOf(Behandlingssteg.FATTE_VEDTAK,
                                                                             Behandlingssteg.IVERKSETT_VEDTAK,
                                                                             Behandlingssteg.AVSLUTTET)) {
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(vedtaksbrevgrunnlag.behandling.ansvarligBeslutter)
        } else {
            null
        }
        val erFeilutbetaltBeløpKorrigertNed =
                varsletBeløp != null && beregningsresultat.totaltFeilutbetaltBeløp < varsletBeløp
        val vedtaksbrevFelles =
                HbVedtaksbrevFelles(brevmetadata = brevmetadata,
                                    fagsaksvedtaksdato = vedtaksbrevgrunnlag.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                                    behandling = hbBehandling,
                                    varsel = HbVarsel(varsletDato, varsletBeløp),
                                    erFeilutbetaltBeløpKorrigertNed = erFeilutbetaltBeløpKorrigertNed,
                                    totaltFeilutbetaltBeløp = beregningsresultat.totaltFeilutbetaltBeløp,
                                    fritekstoppsummering = oppsummeringFritekst,
                                    vedtaksbrevstype = vedtaksbrevtype,
                                    ansvarligBeslutter = ansvarligBeslutter,
                                    hjemmel = hbHjemmel,
                                    totalresultat = hbTotalresultat,
                                    konfigurasjon = HbKonfigurasjon(klagefristIUker = KLAGEFRIST_UKER),
                                    datoer = HbVedtaksbrevDatoer(perioder),
                                    søker = utledSøker(personinfo))
        return HbVedtaksbrevsdata(vedtaksbrevFelles, perioder)
    }

    private fun utledEffektForBruker(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                     beregningsresultat: Beregningsresultat): VedtakHjemmel.EffektForBruker {
        return if (vedtaksbrevgrunnlag.erRevurdering)
            hentEffektForBruker(vedtaksbrevgrunnlag, beregningsresultat.totaltTilbakekrevesMedRenter)
        else VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK
    }

    private fun lagHbTotalresultat(vedtakResultatType: Vedtaksresultat,
                                   beregningsresultat: Beregningsresultat): HbTotalresultat {
        return HbTotalresultat(vedtakResultatType,
                               beregningsresultat.totaltTilbakekrevesUtenRenter,
                               beregningsresultat.totaltTilbakekrevesMedRenter,
                               beregningsresultat.totaltTilbakekrevesBeløpMedRenterUtenSkatt,
                               beregningsresultat.totaltRentebeløp)
    }

    private fun lagHbBehandling(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag): HbBehandling {
        return HbBehandling(erRevurdering = vedtaksbrevgrunnlag.erRevurdering,
                            erRevurderingEtterKlage = vedtaksbrevgrunnlag.erRevurderingEtterKlage,
                            erRevurderingEtterKlageNfp = vedtaksbrevgrunnlag.erRevurderingEtterKlageNfp,
                            originalBehandlingsdatoFagsakvedtak = vedtaksbrevgrunnlag.finnOriginalBehandlingVedtaksdato())
    }

    private fun lagHbVedtaksbrevPerioder(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                         beregningsresultat: Beregningsresultat,
                                         perioderFritekst: List<PeriodeMedTekstDto>): List<HbVedtaksbrevsperiode> {
        val fakta = vedtaksbrevgrunnlag.faktaFeilutbetaling
                    ?: error("Vedtaksbrev mangler fakta for behandling: ${vedtaksbrevgrunnlag.behandling.id}")
        return if (vedtaksbrevgrunnlag.utledVedtaksbrevstype() == Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT) {
            emptyList()
        } else {
            beregningsresultat.beregningsresultatsperioder.map {
                lagBrevdataPeriode(it,
                                   fakta,
                                   vedtaksbrevgrunnlag.vilkårsvurderingsperioder,
                                   vedtaksbrevgrunnlag.vurdertForeldelse,
                                   perioderFritekst)
            }
        }
    }

    private fun hentEffektForBruker(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                    totaltTilbakekrevesMedRenter: BigDecimal): VedtakHjemmel.EffektForBruker {
        val behandlingÅrsak: Behandlingsårsak = vedtaksbrevgrunnlag.behandling.årsaker.first()
        val originaltBeregnetResultat = tilbakekrevingBeregningService.beregn(behandlingÅrsak.originalBehandlingId!!)
        val originalBeregningsresultatsperioder = originaltBeregnetResultat.beregningsresultatsperioder

        val originalBehandlingTotaltMedRenter: BigDecimal = originalBeregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløp }
        val positivtForBruker: Boolean = totaltTilbakekrevesMedRenter < originalBehandlingTotaltMedRenter
        return if (positivtForBruker) {
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER
        } else {
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER
        }
    }

    private fun utledSøker(personinfo: Personinfo): HbPerson {
        return HbPerson(navn = WordUtils.capitalizeFully(personinfo.navn, ' ', '-'),
                        dødsdato = null)
    }

    private fun lagMetadataForVedtaksbrev(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
                                          personinfo: Personinfo,
                                          vedtakResultatType: Vedtaksresultat?,
                                          brevmottager: Brevmottager): Brevmetadata {
        val språkkode: Språkkode = vedtaksbrevgrunnlag.bruker.språkkode
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo,
                                                                              brevmottager,
                                                                              vedtaksbrevgrunnlag.aktivVerge,
                                                                              vedtaksbrevgrunnlag.fagsystem)
        val ytelsesnavn = vedtaksbrevgrunnlag.ytelsestype.navn[språkkode]!!
        val vergeNavn: String = BrevmottagerUtil.getVergenavn(vedtaksbrevgrunnlag.aktivVerge, adresseinfo)
        val tilbakekreves = Vedtaksresultat.FULL_TILBAKEBETALING == vedtakResultatType ||
                            Vedtaksresultat.DELVIS_TILBAKEBETALING == vedtakResultatType
        val ansvarligSaksbehandler = if (vedtaksbrevgrunnlag.aktivtSteg == Behandlingssteg.FORESLÅ_VEDTAK) {
            eksterneDataForBrevService
                    .hentPåloggetSaksbehandlernavnMedDefault(vedtaksbrevgrunnlag.behandling.ansvarligSaksbehandler)
        } else {
            eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrevgrunnlag.behandling.ansvarligSaksbehandler)
        }
        return Brevmetadata(ansvarligSaksbehandler = ansvarligSaksbehandler,
                            behandlendeEnhetId = vedtaksbrevgrunnlag.behandling.behandlendeEnhet,
                            behandlendeEnhetsNavn = vedtaksbrevgrunnlag.behandling.behandlendeEnhetsNavn,
                            mottageradresse = adresseinfo,
                            ytelsestype = vedtaksbrevgrunnlag.ytelsestype,
                            saksnummer = vedtaksbrevgrunnlag.eksternFagsakId,
                            sakspartId = personinfo.ident,
                            sakspartsnavn = personinfo.navn,
                            språkkode = språkkode,
                            tittel = finnTittelVedtaksbrev(ytelsesnavn, tilbakekreves),
                            finnesVerge = vedtaksbrevgrunnlag.harVerge,
                            vergenavn = vergeNavn)
    }

    private fun lagBrevdataPeriode(resultatPeriode: Beregningsresultatsperiode,
                                   fakta: FaktaFeilutbetaling,
                                   vilkårPerioder: Set<Vilkårsvurderingsperiode>,
                                   foreldelse: VurdertForeldelse?,
                                   perioderFritekst: List<PeriodeMedTekstDto>): HbVedtaksbrevsperiode {
        val periode = resultatPeriode.periode
        val fritekster: PeriodeMedTekstDto? = perioderFritekst.firstOrNull { Periode(it.periode) == periode }
        return HbVedtaksbrevsperiode(periode = Handlebarsperiode(periode),
                                     kravgrunnlag = utledKravgrunnlag(resultatPeriode),
                                     fakta = utledFakta(periode, fakta, fritekster),
                                     vurderinger = utledVurderinger(periode, vilkårPerioder, foreldelse, fritekster),
                                     resultat = utledResultat(resultatPeriode, foreldelse))
    }

    private fun utledKravgrunnlag(resultatPeriode: Beregningsresultatsperiode): HbKravgrunnlag {
        return HbKravgrunnlag(resultatPeriode.riktigYtelsesbeløp,
                              resultatPeriode.utbetaltYtelsesbeløp,
                              resultatPeriode.feilutbetaltBeløp)
    }

    private fun utledFakta(periode: Periode, fakta: FaktaFeilutbetaling, fritekst: PeriodeMedTekstDto?): HbFakta {
        return fakta.perioder.first { it.periode.omslutter(periode) }
                .let {
                    HbFakta(it.hendelsestype, it.hendelsesundertype, fritekst?.faktaAvsnitt)
                }
    }

    private fun utledVurderinger(periode: Periode,
                                 vilkårPerioder: Set<Vilkårsvurderingsperiode>,
                                 foreldelse: VurdertForeldelse?,
                                 fritekst: PeriodeMedTekstDto?): HbVurderinger {

        val foreldelsePeriode = finnForeldelsePeriode(foreldelse, periode)
        val vilkårsvurdering = vilkårPerioder.firstOrNull { it.periode.omslutter(periode) }
        val vilkårsvurderingAktsomhet = vilkårsvurdering?.aktsomhet
        val godTro = vilkårsvurdering?.godTro
        val beløpSomErIBehold = godTro?.beløpSomErIBehold
        val aktsomhetsresultat = when {
            foreldelsePeriode?.erForeldet() == true -> AnnenVurdering.FORELDET
            godTro != null -> AnnenVurdering.GOD_TRO
            else -> vilkårsvurderingAktsomhet?.aktsomhet
        }

        val hbSærligeGrunner =
                if (vilkårsvurderingAktsomhet?.skalHaSærligeGrunner == true) {
                    val fritekstSærligeGrunner = fritekst?.særligeGrunnerAvsnitt
                    val fritekstSærligGrunnAnnet = fritekst?.særligeGrunnerAnnetAvsnitt
                    HbSærligeGrunner(vilkårsvurderingAktsomhet.særligeGrunner, fritekstSærligeGrunner, fritekstSærligGrunnAnnet)
                } else {
                    null
                }

        return HbVurderinger(fritekst = fritekst?.vilkårAvsnitt,
                             vilkårsvurderingsresultat = vilkårsvurdering?.vilkårsvurderingsresultat,
                             unntasInnkrevingPgaLavtBeløp = vilkårsvurderingAktsomhet?.tilbakekrevSmåbeløp == false,
                             særligeGrunner = hbSærligeGrunner,
                             aktsomhetsresultat = aktsomhetsresultat,
                             beløpIBehold = beløpSomErIBehold,
                             foreldelsevurdering = foreldelsePeriode?.foreldelsesvurderingstype
                                                   ?: Foreldelsesvurderingstype.IKKE_VURDERT,
                             foreldelsesfrist = foreldelsePeriode?.foreldelsesfrist,
                             oppdagelsesdato = foreldelsePeriode?.oppdagelsesdato,
                             fritekstForeldelse = fritekst?.foreldelseAvsnitt)
    }

    private fun utledResultat(resultatPeriode: Beregningsresultatsperiode, foreldelse: VurdertForeldelse?): HbResultat {
        val foreldelsePeriode = finnForeldelsePeriode(foreldelse, resultatPeriode.periode)
        val foreldetPeriode = foreldelsePeriode != null && foreldelsePeriode.erForeldet()

        return HbResultat(tilbakekrevesBeløp = resultatPeriode.tilbakekrevingsbeløpUtenRenter,
                          tilbakekrevesBeløpUtenSkattMedRenter = resultatPeriode.tilbakekrevingsbeløpEtterSkatt,
                          rentebeløp = resultatPeriode.rentebeløp,
                          foreldetBeløp =
                          if (foreldetPeriode) {
                              resultatPeriode.feilutbetaltBeløp.subtract(resultatPeriode.tilbakekrevingsbeløp)
                          } else null)
    }

    private fun finnForeldelsePeriode(foreldelse: VurdertForeldelse?, periode: Periode): Foreldelsesperiode? {
        return if (foreldelse == null) {
            null
        } else foreldelse.foreldelsesperioder
                       .firstOrNull { p -> p.periode.omslutter(periode) }
               ?: error("Fant ikke VurdertForeldelse-periode som omslutter periode $periode")
    }

    private fun finnTittelVedtaksbrev(ytelsesnavn: String, tilbakekreves: Boolean): String {
        return if (tilbakekreves) {
            TITTEL_VEDTAK_TILBAKEBETALING + ytelsesnavn
        } else {
            TITTEL_VEDTAK_INGEN_TILBAKEBETALING + ytelsesnavn
        }
    }

    companion object {

        private const val TITTEL_VEDTAK_TILBAKEBETALING = "Vedtak tilbakebetaling "
        private const val TITTEL_VEDTAK_INGEN_TILBAKEBETALING = "Vedtak ingen tilbakebetaling "
        private const val KLAGEFRIST_UKER = 6
    }
}
