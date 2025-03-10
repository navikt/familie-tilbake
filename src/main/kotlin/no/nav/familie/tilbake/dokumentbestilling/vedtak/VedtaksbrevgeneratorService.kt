package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat.DELVIS_TILBAKEBETALING
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat.FULL_TILBAKEBETALING
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.HbGrunnbeløpUtil.lagHbGrunnbeløp
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
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HarBrukerUttaltSeg
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.kontrakter.Månedsperiode
import no.nav.tilbakekreving.kontrakter.Språkkode
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Ytelsestype
import org.apache.commons.text.WordUtils
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class VedtaksbrevgeneratorService(
    private val tilbakekrevingBeregningService: TilbakekrevingsberegningService,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val organisasjonService: OrganisasjonService,
    private val brevmetadataUtil: BrevmetadataUtil,
    private val periodeService: PeriodeService,
) {
    fun genererVedtaksbrevForSending(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        brevmottager: Brevmottager,
        forhåndsgenerertMetadata: Brevmetadata? = null,
        logContext: SecureLog.Context,
    ): Brevdata {
        val behandlingId = vedtaksbrevgrunnlag.behandling.id
        val vedtaksbrevsdata = hentDataForVedtaksbrev(vedtaksbrevgrunnlag, brevmottager, forhåndsgenerertMetadata, logContext)
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata
        val erPerioderSammenslått = periodeService.erPerioderSammenslått(behandlingId)

        val data =
            Fritekstbrevsdata(
                TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
                TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata, erPerioderSammenslått),
                vedtaksbrevsdata.metadata,
            )
        val vedleggHtml =
            if (vedtaksbrevsdata.vedtaksbrevsdata.felles.harVedlegg) {
                TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
            } else {
                ""
            }
        return Brevdata(
            mottager = brevmottager,
            metadata = data.brevmetadata,
            overskrift = data.overskrift,
            brevtekst = data.brevtekst,
            vedleggHtml = vedleggHtml,
        )
    }

    fun genererVedtaksbrevForForhåndsvisning(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        hentForhåndvisningVedtaksbrevPdfDto: HentForhåndvisningVedtaksbrevPdfDto,
        logContext: SecureLog.Context,
    ): Brevdata {
        val behandlingId = vedtaksbrevgrunnlag.behandling.id

        val erPerioderSammenslått = periodeService.erPerioderSammenslått(behandlingId)

        val (brevmetadata, brevmottager) =
            brevmetadataUtil.lagBrevmetadataForMottakerTilForhåndsvisning(vedtaksbrevgrunnlag)
        val vedtaksbrevsdata =
            hentDataForVedtaksbrev(
                vedtaksbrevgrunnlag,
                hentForhåndvisningVedtaksbrevPdfDto.oppsummeringstekst,
                hentForhåndvisningVedtaksbrevPdfDto.perioderMedTekst,
                brevmottager,
                brevmetadata,
                logContext,
            )
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata

        val vedleggHtml =
            if (hbVedtaksbrevsdata.felles.harVedlegg) {
                TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
            } else {
                ""
            }
        return Brevdata(
            mottager = brevmottager,
            metadata = vedtaksbrevsdata.metadata,
            overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
            brevtekst = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata, erPerioderSammenslått),
            vedleggHtml = vedleggHtml,
        )
    }

    fun genererVedtaksbrevsdataTilVisningIFrontendSkjema(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        logContext: SecureLog.Context,
    ): HbVedtaksbrevsdata {
        val (brevmetadata, brevmottager) =
            brevmetadataUtil.lagBrevmetadataForMottakerTilForhåndsvisning(vedtaksbrevgrunnlag)
        val vedtaksbrevsdata = hentDataForVedtaksbrev(vedtaksbrevgrunnlag, brevmottager, brevmetadata, logContext)
        return vedtaksbrevsdata.vedtaksbrevsdata
    }

    private fun hentDataForVedtaksbrev(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        brevmottager: Brevmottager,
        brevmetadata: Brevmetadata? = null,
        logContext: SecureLog.Context,
    ): Vedtaksbrevsdata {
        val fritekstoppsummering = vedtaksbrevgrunnlag.behandling.vedtaksbrevOppsummering?.oppsummeringFritekst
        val fritekstPerioder: List<PeriodeMedTekstDto> =
            VedtaksbrevFritekstMapper.mapFritekstFraDb(vedtaksbrevgrunnlag.behandling.eksisterendePerioderForBrev)
        return hentDataForVedtaksbrev(
            vedtaksbrevgrunnlag,
            fritekstoppsummering,
            fritekstPerioder,
            brevmottager,
            brevmetadata,
            logContext,
        )
    }

    private fun hentDataForVedtaksbrev(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        oppsummeringFritekst: String?,
        perioderFritekst: List<PeriodeMedTekstDto>,
        brevmottager: Brevmottager,
        forhåndsgenerertMetadata: Brevmetadata? = null,
        logContext: SecureLog.Context,
    ): Vedtaksbrevsdata {
        val språkkode: Språkkode = vedtaksbrevgrunnlag.bruker.språkkode
        val personinfo: Personinfo =
            eksterneDataForBrevService.hentPerson(
                vedtaksbrevgrunnlag.bruker.ident,
                vedtaksbrevgrunnlag.fagsystem,
                logContext,
            )
        val beregnetResultat = tilbakekrevingBeregningService.beregn(vedtaksbrevgrunnlag.behandling.id)
        val brevMetadata: Brevmetadata =
            (
                forhåndsgenerertMetadata ?: lagMetadataForVedtaksbrev(
                    vedtaksbrevgrunnlag,
                    personinfo,
                    brevmottager,
                    språkkode,
                    logContext,
                )
            ).copy(
                tittel =
                    finnTittelVedtaksbrev(
                        ytelsesnavn = vedtaksbrevgrunnlag.ytelsestype.navn[språkkode]!!,
                        tilbakekreves =
                            beregnetResultat.vedtaksresultat == FULL_TILBAKEBETALING ||
                                beregnetResultat.vedtaksresultat == DELVIS_TILBAKEBETALING,
                    ),
            )
        val data: HbVedtaksbrevsdata =
            lagHbVedtaksbrevsdata(
                vedtaksbrevgrunnlag,
                personinfo,
                beregnetResultat,
                oppsummeringFritekst,
                perioderFritekst,
                brevMetadata,
                logContext,
            )
        return Vedtaksbrevsdata(data, brevMetadata)
    }

    private fun lagHbVedtaksbrevsdata(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        personinfo: Personinfo,
        beregningsresultat: Beregningsresultat,
        oppsummeringFritekst: String?,
        perioderFritekst: List<PeriodeMedTekstDto>,
        brevmetadata: Brevmetadata,
        logContext: SecureLog.Context,
    ): HbVedtaksbrevsdata {
        val vedtaksbrevtype = vedtaksbrevgrunnlag.utledVedtaksbrevstype()
        val effektForBruker: VedtakHjemmel.EffektForBruker =
            utledEffektForBruker(vedtaksbrevgrunnlag, beregningsresultat)
        val klagebehandling = vedtaksbrevgrunnlag.klagebehandling
        val hbHjemmel =
            VedtakHjemmel.lagHjemmel(
                beregningsresultat.vedtaksresultat,
                vedtaksbrevgrunnlag,
                effektForBruker,
                brevmetadata.språkkode,
                visHjemmelForRenter = true,
                klagebehandling,
            ) // sannsynligvis hjemmel

        val fakta =
            vedtaksbrevgrunnlag.faktaFeilutbetaling
                ?: error("Vedtaksbrev mangler fakta for behandling: ${vedtaksbrevgrunnlag.behandling.id}")

        val perioder: List<HbVedtaksbrevsperiode> =
            lagHbVedtaksbrevPerioder(
                vedtaksbrevgrunnlag,
                beregningsresultat,
                perioderFritekst,
                fakta,
            )
        val hbTotalresultat: HbTotalresultat =
            lagHbTotalresultat(beregningsresultat.vedtaksresultat, beregningsresultat)
        val hbBehandling: HbBehandling = lagHbBehandling(vedtaksbrevgrunnlag)
        val varsletBeløp = vedtaksbrevgrunnlag.varsletBeløp
        val varsletDato =
            vedtaksbrevgrunnlag.sisteVarsel
                ?.sporbar
                ?.opprettetTid
                ?.toLocalDate()
        val ansvarligBeslutter = finnAnsvarligBeslutter(vedtaksbrevgrunnlag, logContext)
        val erFeilutbetaltBeløpKorrigertNed =
            varsletBeløp != null && beregningsresultat.totaltFeilutbetaltBeløp < varsletBeløp

        val klagefristIUker =
            when (brevmetadata.ytelsestype) {
                Ytelsestype.KONTANTSTØTTE -> KLAGEFRIST_UKER_KONTANTSTØTTE
                else -> KLAGEFRIST_UKER
            }

        val vedtaksbrevFelles =
            HbVedtaksbrevFelles(
                brevmetadata = brevmetadata,
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
                konfigurasjon = HbKonfigurasjon(klagefristIUker = klagefristIUker),
                datoer = HbVedtaksbrevDatoer(perioder),
                søker = utledSøker(personinfo),
                harBrukerUttaltSeg = fakta.vurderingAvBrukersUttalelse?.harBrukerUttaltSeg ?: HarBrukerUttaltSeg.IKKE_VURDERT,
            )
        return HbVedtaksbrevsdata(vedtaksbrevFelles, perioder)
    }

    private fun finnAnsvarligBeslutter(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        logContext: SecureLog.Context,
    ) = when {
        harAnsvarligBeslutter(vedtaksbrevgrunnlag) -> hentAnsvarligBeslutterNavn(vedtaksbrevgrunnlag, logContext)
        else -> null
    }

    private fun hentAnsvarligBeslutterNavn(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        logContext: SecureLog.Context,
    ) = eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(vedtaksbrevgrunnlag.behandling.ansvarligBeslutter, logContext)

    private fun saksbehandlingstypeHarBeslutter(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag) =
        when (vedtaksbrevgrunnlag.behandling.saksbehandlingstype) {
            Saksbehandlingstype.ORDINÆR -> true
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR -> false
            Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP -> false
        }

    private fun harAnsvarligBeslutter(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag) = saksbehandlingstypeHarBeslutter(vedtaksbrevgrunnlag) && erBesluttet(vedtaksbrevgrunnlag)

    private fun erBesluttet(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag) =
        vedtaksbrevgrunnlag.aktivtSteg in
            setOf(
                Behandlingssteg.FATTE_VEDTAK,
                Behandlingssteg.IVERKSETT_VEDTAK,
                Behandlingssteg.AVSLUTTET,
            )

    private fun utledEffektForBruker(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        beregningsresultat: Beregningsresultat,
    ): VedtakHjemmel.EffektForBruker =
        if (vedtaksbrevgrunnlag.erRevurdering) {
            hentEffektForBruker(vedtaksbrevgrunnlag, beregningsresultat.totaltTilbakekrevesMedRenter)
        } else {
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK
        }

    private fun lagHbTotalresultat(
        vedtakResultatType: Vedtaksresultat,
        beregningsresultat: Beregningsresultat,
    ): HbTotalresultat =
        HbTotalresultat(
            vedtakResultatType,
            beregningsresultat.totaltTilbakekrevesUtenRenter,
            beregningsresultat.totaltTilbakekrevesMedRenter,
            beregningsresultat.totaltTilbakekrevesBeløpMedRenterUtenSkatt,
            beregningsresultat.totaltRentebeløp,
        )

    private fun lagHbBehandling(vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag): HbBehandling =
        HbBehandling(
            erRevurdering = vedtaksbrevgrunnlag.erRevurdering,
            erRevurderingEtterKlage = vedtaksbrevgrunnlag.erRevurderingEtterKlage,
            erRevurderingEtterKlageNfp = vedtaksbrevgrunnlag.erRevurderingEtterKlageNfp,
            originalBehandlingsdatoFagsakvedtak = vedtaksbrevgrunnlag.finnOriginalBehandlingVedtaksdato(),
        )

    private fun lagHbVedtaksbrevPerioder(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        beregningsresultat: Beregningsresultat,
        perioderFritekst: List<PeriodeMedTekstDto>,
        fakta: FaktaFeilutbetaling,
    ): List<HbVedtaksbrevsperiode> =
        if (vedtaksbrevgrunnlag.utledVedtaksbrevstype() == Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT) {
            emptyList()
        } else {
            beregningsresultat.beregningsresultatsperioder.mapIndexed { index, it ->
                lagBrevdataPeriode(
                    resultatPeriode = it,
                    fakta = fakta,
                    vilkårPerioder = vedtaksbrevgrunnlag.vilkårsvurderingsperioder,
                    foreldelse = vedtaksbrevgrunnlag.vurdertForeldelse,
                    perioderFritekst = perioderFritekst,
                    førstePeriode = index == 0,
                )
            }
        }

    private fun hentEffektForBruker(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        totaltTilbakekrevesMedRenter: BigDecimal,
    ): VedtakHjemmel.EffektForBruker {
        val behandlingÅrsak: Behandlingsårsak = vedtaksbrevgrunnlag.behandling.årsaker.first()
        val originaltBeregnetResultat = tilbakekrevingBeregningService.beregn(behandlingÅrsak.originalBehandlingId!!)
        val originalBeregningsresultatsperioder = originaltBeregnetResultat.beregningsresultatsperioder

        val originalBehandlingTotaltMedRenter: BigDecimal =
            originalBeregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløp }
        val positivtForBruker: Boolean = totaltTilbakekrevesMedRenter < originalBehandlingTotaltMedRenter
        return if (positivtForBruker) {
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER
        } else {
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER
        }
    }

    private fun utledSøker(personinfo: Personinfo): HbPerson =
        HbPerson(
            navn = WordUtils.capitalizeFully(personinfo.navn, ' ', '-'),
        )

    private fun lagMetadataForVedtaksbrev(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        personinfo: Personinfo,
        brevmottager: Brevmottager,
        språkkode: Språkkode,
        logContext: SecureLog.Context,
    ): Brevmetadata {
        val adresseinfo: Adresseinfo =
            eksterneDataForBrevService.hentAdresse(
                personinfo,
                brevmottager,
                vedtaksbrevgrunnlag.aktivVerge,
                vedtaksbrevgrunnlag.fagsystem,
                logContext,
            )
        val vergeNavn: String = BrevmottagerUtil.getVergenavn(vedtaksbrevgrunnlag.aktivVerge, adresseinfo)
        val ansvarligSaksbehandler =
            if (vedtaksbrevgrunnlag.aktivtSteg == Behandlingssteg.FORESLÅ_VEDTAK) {
                eksterneDataForBrevService
                    .hentPåloggetSaksbehandlernavnMedDefault(vedtaksbrevgrunnlag.behandling.ansvarligSaksbehandler, logContext)
            } else {
                eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrevgrunnlag.behandling.ansvarligSaksbehandler)
            }
        return Brevmetadata(
            sakspartId = personinfo.ident,
            sakspartsnavn = personinfo.navn,
            finnesVerge = vedtaksbrevgrunnlag.harVerge,
            vergenavn = vergeNavn,
            mottageradresse = adresseinfo,
            behandlendeEnhetId = vedtaksbrevgrunnlag.behandling.behandlendeEnhet,
            behandlendeEnhetsNavn = vedtaksbrevgrunnlag.behandling.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            saksnummer = vedtaksbrevgrunnlag.eksternFagsakId,
            språkkode = språkkode,
            ytelsestype = vedtaksbrevgrunnlag.ytelsestype,
            gjelderDødsfall = personinfo.dødsdato != null,
            institusjon =
                vedtaksbrevgrunnlag.institusjon?.let {
                    organisasjonService.mapTilInstitusjonForBrevgenerering(
                        it.organisasjonsnummer,
                    )
                },
        )
    }

    private fun lagBrevdataPeriode(
        resultatPeriode: Beregningsresultatsperiode,
        fakta: FaktaFeilutbetaling,
        vilkårPerioder: Set<Vilkårsvurderingsperiode>,
        foreldelse: VurdertForeldelse?,
        perioderFritekst: List<PeriodeMedTekstDto>,
        førstePeriode: Boolean,
    ): HbVedtaksbrevsperiode {
        val periode = resultatPeriode.periode
        val fritekster: PeriodeMedTekstDto? =
            perioderFritekst.firstOrNull { Månedsperiode(it.periode.fom, it.periode.tom) == periode }
        val hbFakta = utledFakta(periode, fakta, fritekster)
        val skalBrukeGrunnbeløpIVedtaksbrev = hbFakta.hendelsesundertype == Hendelsesundertype.INNTEKT_OVER_6G
        return HbVedtaksbrevsperiode(
            periode = periode.toDatoperiode(),
            kravgrunnlag = utledKravgrunnlag(resultatPeriode),
            fakta = hbFakta,
            vurderinger = utledVurderinger(periode, vilkårPerioder, foreldelse, fritekster),
            resultat = utledResultat(resultatPeriode, foreldelse),
            førstePeriode = førstePeriode,
            grunnbeløp = if (skalBrukeGrunnbeløpIVedtaksbrev) lagHbGrunnbeløp(periode) else null,
        )
    }

    private fun utledKravgrunnlag(resultatPeriode: Beregningsresultatsperiode): HbKravgrunnlag =
        HbKravgrunnlag(
            resultatPeriode.riktigYtelsesbeløp,
            resultatPeriode.utbetaltYtelsesbeløp,
            resultatPeriode.feilutbetaltBeløp,
        )

    private fun utledFakta(
        periode: Månedsperiode,
        fakta: FaktaFeilutbetaling,
        fritekst: PeriodeMedTekstDto?,
    ): HbFakta =
        fakta.perioder
            .first { it.periode.inneholder(periode) }
            .let {
                HbFakta(it.hendelsestype, it.hendelsesundertype, fritekst?.faktaAvsnitt)
            }

    private fun utledVurderinger(
        periode: Månedsperiode,
        vilkårPerioder: Set<Vilkårsvurderingsperiode>,
        foreldelse: VurdertForeldelse?,
        fritekst: PeriodeMedTekstDto?,
    ): HbVurderinger {
        val foreldelsePeriode = finnForeldelsePeriode(foreldelse, periode)
        val vilkårsvurdering = vilkårPerioder.firstOrNull { it.periode.inneholder(periode) }
        val vilkårsvurderingAktsomhet = vilkårsvurdering?.aktsomhet
        val godTro = vilkårsvurdering?.godTro
        val beløpSomErIBehold = godTro?.beløpSomErIBehold
        val aktsomhetsresultat =
            when {
                foreldelsePeriode?.erForeldet() == true -> AnnenVurdering.FORELDET
                godTro != null -> AnnenVurdering.GOD_TRO
                else -> vilkårsvurderingAktsomhet?.aktsomhet
            }

        val hbSærligeGrunner =
            if (vilkårsvurderingAktsomhet?.skalHaSærligeGrunner == true) {
                val fritekstSærligeGrunner = fritekst?.særligeGrunnerAvsnitt
                val fritekstSærligGrunnAnnet = fritekst?.særligeGrunnerAnnetAvsnitt
                HbSærligeGrunner(
                    vilkårsvurderingAktsomhet.særligeGrunner,
                    fritekstSærligeGrunner,
                    fritekstSærligGrunnAnnet,
                )
            } else {
                null
            }

        return HbVurderinger(
            fritekst = fritekst?.vilkårAvsnitt,
            vilkårsvurderingsresultat = vilkårsvurdering?.vilkårsvurderingsresultat,
            unntasInnkrevingPgaLavtBeløp = vilkårsvurderingAktsomhet?.tilbakekrevSmåbeløp == false,
            særligeGrunner = hbSærligeGrunner,
            aktsomhetsresultat = aktsomhetsresultat,
            beløpIBehold = beløpSomErIBehold,
            foreldelsevurdering =
                foreldelsePeriode?.foreldelsesvurderingstype
                    ?: Foreldelsesvurderingstype.IKKE_VURDERT,
            foreldelsesfrist = foreldelsePeriode?.foreldelsesfrist,
            oppdagelsesdato = foreldelsePeriode?.oppdagelsesdato,
            fritekstForeldelse = fritekst?.foreldelseAvsnitt,
        )
    }

    private fun utledResultat(
        resultatPeriode: Beregningsresultatsperiode,
        foreldelse: VurdertForeldelse?,
    ): HbResultat {
        val foreldelsePeriode = finnForeldelsePeriode(foreldelse, resultatPeriode.periode)
        val foreldetPeriode = foreldelsePeriode != null && foreldelsePeriode.erForeldet()

        return HbResultat(
            tilbakekrevesBeløp = resultatPeriode.tilbakekrevingsbeløpUtenRenter,
            tilbakekrevesBeløpUtenSkattMedRenter = resultatPeriode.tilbakekrevingsbeløpEtterSkatt,
            rentebeløp = resultatPeriode.rentebeløp,
            foreldetBeløp =
                if (foreldetPeriode) {
                    resultatPeriode.feilutbetaltBeløp.subtract(resultatPeriode.tilbakekrevingsbeløp)
                } else {
                    null
                },
        )
    }

    private fun finnForeldelsePeriode(
        foreldelse: VurdertForeldelse?,
        periode: Månedsperiode,
    ): Foreldelsesperiode? =
        if (foreldelse == null) {
            null
        } else {
            foreldelse.foreldelsesperioder
                .firstOrNull { p -> p.periode.inneholder(periode) }
                ?: error("Fant ikke VurdertForeldelse-periode som omslutter periode $periode")
        }

    private fun finnTittelVedtaksbrev(
        ytelsesnavn: String,
        tilbakekreves: Boolean,
    ): String =
        if (tilbakekreves) {
            TITTEL_VEDTAK_TILBAKEBETALING + ytelsesnavn
        } else {
            TITTEL_VEDTAK_INGEN_TILBAKEBETALING + ytelsesnavn
        }

    companion object {
        private const val TITTEL_VEDTAK_TILBAKEBETALING = "Vedtak tilbakebetaling "
        private const val TITTEL_VEDTAK_INGEN_TILBAKEBETALING = "Vedtak ingen tilbakebetaling "
        private const val KLAGEFRIST_UKER = 6
        private const val KLAGEFRIST_UKER_KONTANTSTØTTE = 3
    }
}
