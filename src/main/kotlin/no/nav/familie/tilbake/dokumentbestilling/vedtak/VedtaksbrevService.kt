package no.nav.familie.tilbake.dokumentbestilling.vedtak

import com.github.jknack.handlebars.internal.text.WordUtils
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.*
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.*
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.*
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class VedtaksbrevService(private val behandlingRepository: BehandlingRepository,
                         private val faktaRepository: FaktaFeilutbetalingRepository,
                         private val foreldelseRepository: VurdertForeldelseRepository,
                         private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                         private val fagsakRepository: FagsakRepository,
                         private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
                         private val vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository,
                         private val brevsporingService: BrevsporingService,
                         private val tilbakekrevingBeregningService: TilbakekrevingsberegningService,
                         private val eksterneDataForBrevService: EksterneDataForBrevService,
                         private val pdfBrevService: PdfBrevService) {

    fun sendVedtaksbrev(behandling: Behandling, brevmottager: Brevmottager) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevsdata = hentDataForVedtaksbrev(behandling, fagsak, brevmottager)
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata
        val data = Fritekstbrevsdata(TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
                                     TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata),
                                     vedtaksbrevsdata.metadata)
        val vedleggHtml = if (vedtaksbrevsdata.vedtaksbrevsdata.felles.harVedlegg) {
            TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
        } else ""
        val brevdata = Brevdata(mottager = brevmottager,
                                metadata = data.brevmetadata,
                                overskrift = data.overskrift,
                                brevtekst = data.brevtekst,
                                vedleggHtml = vedleggHtml)
        pdfBrevService.sendBrev(behandling,
                                fagsak,
                                Brevtype.VEDTAK,
                                brevdata)
    }

    fun hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto: HentForhåndvisningVedtaksbrevPdfDto): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(dto.behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevsdata = hentDataForVedtaksbrev(behandling,
                                                      fagsak,
                                                      dto.oppsummeringstekst,
                                                      dto.perioderMedTekst,
                                                      getBrevmottager(behandling))
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata

        val vedleggHtml = if (hbVedtaksbrevsdata.felles.harVedlegg) {
            TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata.vedtaksbrevsdata)
        } else ""

        val brevdata =
                Brevdata(mottager = getBrevmottager(behandling),
                         metadata = vedtaksbrevsdata.metadata,
                         overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata),
                         brevtekst = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(hbVedtaksbrevsdata),
                         vedleggHtml = vedleggHtml)
        return pdfBrevService.genererForhåndsvisning(brevdata)
    }

    fun hentVedtaksbrevSomTekst(behandlingId: UUID): List<Avsnitt> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevsdata = hentDataForVedtaksbrev(behandling, fagsak, getBrevmottager(behandling))
        val hbVedtaksbrevsdata: HbVedtaksbrevsdata = vedtaksbrevsdata.vedtaksbrevsdata
        val hovedoverskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata)
        return AvsnittUtil.lagVedtaksbrevDeltIAvsnitt(hbVedtaksbrevsdata, hovedoverskrift)
    }

    @Transactional
    fun lagreFriteksterFraSaksbehandler(behandlingId: UUID, fritekstavsnittDto: FritekstavsnittDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val vedtaksbrevstype = behandling.utledVedtaksbrevstype()
        val vedtaksbrevsoppsummering = VedtaksbrevFritekstMapper.tilDomene(behandlingId, fritekstavsnittDto.oppsummeringstekst)
        val vedtaksbrevsperioder = VedtaksbrevFritekstMapper
                .tilDomeneVedtaksbrevsperiode(behandlingId, fritekstavsnittDto.perioderMedTekst)

        // Valider om obligatoriske fritekster er satt
        val faktaFeilutbetaling = faktaRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        VedtaksbrevFritekstValidator.validerObligatoriskeFritekster(behandling = behandling,
                                                                    faktaFeilutbetaling = faktaFeilutbetaling,
                                                                    vilkårsvurdering = vilkårsvurdering,
                                                                    vedtaksbrevFritekstPerioder = vedtaksbrevsperioder,
                                                                    avsnittMedPerioder = fritekstavsnittDto.perioderMedTekst,
                                                                    vedtaksbrevsoppsummering = vedtaksbrevsoppsummering,
                                                                    vedtaksbrevstype = vedtaksbrevstype)
        // slett og legge til Vedtaksbrevsoppsummering
        val eksisterendeVedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        if (eksisterendeVedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.delete(eksisterendeVedtaksbrevsoppsummering)
        }
        vedtaksbrevsoppsummeringRepository.insert(vedtaksbrevsoppsummering)

        // slett og legge til Vedtaksbrevsperiode
        val eksisterendeVedtaksbrevperioder = vedtaksbrevsperiodeRepository.findByBehandlingId(behandlingId)
        eksisterendeVedtaksbrevperioder.forEach { vedtaksbrevsperiodeRepository.deleteById(it.id) }
        vedtaksbrevsperioder.forEach { vedtaksbrevsperiodeRepository.insert(it) }
    }

    @Transactional
    fun deaktiverEksisterendeVedtaksbrevdata(behandlingId: UUID) {
        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
                ?.let { vedtaksbrevsoppsummeringRepository.deleteById(it.id) }
        vedtaksbrevsperiodeRepository.findByBehandlingId(behandlingId).forEach { vedtaksbrevsperiodeRepository.deleteById(it.id) }
    }

    private fun getBrevmottager(behandlingId: Behandling): Brevmottager {
        return if (behandlingId.harVerge) Brevmottager.VERGE else Brevmottager.BRUKER
    }

    private fun hentDataForVedtaksbrev(behandling: Behandling,
                                       fagsak: Fagsak,
                                       brevmottager: Brevmottager): Vedtaksbrevsdata {
        val fritekstoppsummering = hentOppsummeringFritekst(behandling.id)
        val fritekstPerioder: List<PeriodeMedTekstDto> = hentFriteksterTilPerioder(behandling.id)
        return hentDataForVedtaksbrev(behandling, fagsak, fritekstoppsummering, fritekstPerioder, brevmottager)
    }

    private fun hentDataForVedtaksbrev(behandling: Behandling,
                                       fagsak: Fagsak,
                                       oppsummeringFritekst: String?,
                                       perioderFritekst: List<PeriodeMedTekstDto>,
                                       brevmottager: Brevmottager): Vedtaksbrevsdata {
        val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val beregnetResultat = tilbakekrevingBeregningService.beregn(behandling.id)
        val brevMetadata: Brevmetadata = lagMetadataForVedtaksbrev(behandling,
                                                                   fagsak,
                                                                   personinfo,
                                                                   beregnetResultat.vedtaksresultat,
                                                                   brevmottager)
        val data: HbVedtaksbrevsdata = lagHbVedtaksbrevsdata(behandling,
                                                             fagsak,
                                                             personinfo,
                                                             beregnetResultat,
                                                             oppsummeringFritekst,
                                                             perioderFritekst,
                                                             brevMetadata)
        return Vedtaksbrevsdata(data, brevMetadata)
    }

    private fun lagHbVedtaksbrevsdata(behandling: Behandling,
                                      fagsak: Fagsak,
                                      personinfo: Personinfo,
                                      beregnetResultat: Beregningsresultat,
                                      oppsummeringFritekst: String?,
                                      perioderFritekst: List<PeriodeMedTekstDto>,
                                      brevmetadata: Brevmetadata): HbVedtaksbrevsdata {
        val resulatPerioder = beregnetResultat.beregningsresultatsperioder
        val vedtakResultatType = beregnetResultat.vedtaksresultat
        val vilkårPerioder = finnVilkårsvurderingsperioder(behandling.id)
        val foreldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        val vedtaksbrevtype = behandling.utledVedtaksbrevstype()
        val hbVedtaksResultatBeløp = HbVedtaksResultatBeløp(resulatPerioder)
        val effektForBruker: VedtakHjemmel.EffektForBruker = utledEffektForBruker(behandling, hbVedtaksResultatBeløp)
        val klagebehandling = behandling.sisteÅrsak?.type == Behandlingsårsakstype.REVURDERING_KLAGE_NFP
        val hbHjemmel = VedtakHjemmel.lagHjemmel(vedtakResultatType,
                                                 foreldelse,
                                                 vilkårPerioder,
                                                 effektForBruker,
                                                 fagsak.ytelsestype,
                                                 brevmetadata.språkkode,
                                                 visHjemmelForRenter = true,
                                                 klagebehandling) // sannsynligvis hjemmel
        val perioder: List<HbVedtaksbrevsperiode> = lagHbVedtaksbrevPerioder(behandling.id,
                                                                             perioderFritekst,
                                                                             resulatPerioder,
                                                                             vilkårPerioder,
                                                                             foreldelse,
                                                                             vedtaksbrevtype)
        val hbTotalresultat: HbTotalresultat = lagHbTotalresultat(vedtakResultatType, hbVedtaksResultatBeløp)
        val hbBehandling: HbBehandling = lagHbBehandling(behandling)
        val varsletBeløp = finnVarsletBeløp(behandling)
        val varsletDato = finnVarsletDato(behandling.id)
        val ansvarligBeslutter = behandling.ansvarligBeslutter?.let { eksterneDataForBrevService.hentSaksbehandlernavn(it) }
        val erFeilutbetaltBeløpKorrigertNed =
                varsletBeløp != null && hbVedtaksResultatBeløp.totaltFeilutbetaltBeløp < varsletBeløp
        val vedtaksbrevFelles =
                HbVedtaksbrevFelles(brevmetadata = brevmetadata,
                                    fagsaksvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                                    behandling = hbBehandling,
                                    varsel = HbVarsel(varsletDato, varsletBeløp),
                                    erFeilutbetaltBeløpKorrigertNed = erFeilutbetaltBeløpKorrigertNed,
                                    totaltFeilutbetaltBeløp = hbVedtaksResultatBeløp.totaltFeilutbetaltBeløp,
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

    private fun utledEffektForBruker(behandling: Behandling,
                                     hbVedtaksResultatBeløp: HbVedtaksResultatBeløp): VedtakHjemmel.EffektForBruker {
        return if (behandling.erRevurdering) hentEffektForBruker(behandling, hbVedtaksResultatBeløp.totaltTilbakekrevesMedRenter)
        else VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK
    }

    private fun lagHbTotalresultat(vedtakResultatType: Vedtaksresultat,
                                   hbVedtaksResultatBeløp: HbVedtaksResultatBeløp): HbTotalresultat {
        return HbTotalresultat(vedtakResultatType,
                               hbVedtaksResultatBeløp.totaltTilbakekrevesUtenRenter,
                               hbVedtaksResultatBeløp.totaltTilbakekrevesMedRenter,
                               hbVedtaksResultatBeløp.totaltTilbakekrevesBeløpMedRenterUtenSkatt,
                               hbVedtaksResultatBeløp.totaltRentebeløp)
    }

    private fun lagHbBehandling(behandling: Behandling): HbBehandling {
        val erRevurderingEtterKlage: Boolean = behandling.erRevurdering && behandling.årsaker
                .any { it.type in setOf(Behandlingsårsakstype.REVURDERING_KLAGE_KA, Behandlingsårsakstype.REVURDERING_KLAGE_NFP) }
        val erRevurderingEtterKlageNfp: Boolean =
                behandling.erRevurdering && behandling.årsaker.any { it.type == Behandlingsårsakstype.REVURDERING_KLAGE_NFP }
        val originalBehandlingVedtaksdato = if (behandling.erRevurdering) finnOriginalBehandlingVedtaksdato(behandling) else null
        return HbBehandling(erRevurdering = behandling.erRevurdering,
                            erRevurderingEtterKlage = erRevurderingEtterKlage,
                            erRevurderingEtterKlageNfp = erRevurderingEtterKlageNfp,
                            originalBehandlingsdatoFagsakvedtak = originalBehandlingVedtaksdato)
    }

    private fun lagHbVedtaksbrevPerioder(behandlingId: UUID,
                                         perioderFritekst: List<PeriodeMedTekstDto>,
                                         resulatPerioder: List<Beregningsresultatsperiode>,
                                         vilkårPerioder: Set<Vilkårsvurderingsperiode>,
                                         foreldelse: VurdertForeldelse?,
                                         vedtaksbrevtype: Vedtaksbrevstype): List<HbVedtaksbrevsperiode> {
        val fakta: FaktaFeilutbetaling = faktaRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
                                         ?: error("Vedtaksbrev mangler fakta for behandling: $behandlingId")
        return if (vedtaksbrevtype == Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT) emptyList()
        else resulatPerioder.map {
            lagBrevdataPeriode(it, fakta, vilkårPerioder, foreldelse, perioderFritekst)
        }
    }

    private fun hentEffektForBruker(behandling: Behandling,
                                    totaltTilbakekrevesMedRenter: BigDecimal): VedtakHjemmel.EffektForBruker {
        val behandlingÅrsak: Behandlingsårsak = behandling.årsaker.first()
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

    private fun finnOriginalBehandlingVedtaksdato(behandling: Behandling): LocalDate {
        val behandlingÅrsak = behandling.årsaker.first()
        behandlingÅrsak.originalBehandlingId ?: error("Mangler orginalBehandlingId for behandling: ${behandling.id}")

        return behandlingRepository.findByIdOrThrow(behandlingÅrsak.originalBehandlingId)
                       .sisteResultat
                       ?.behandlingsvedtak
                       ?.vedtaksdato
               ?: error("Mangler vedtaksdato for orginal behandling med id : ${behandlingÅrsak.originalBehandlingId}")
    }

    private fun utledSøker(personinfo: Personinfo): HbPerson {
        return HbPerson(navn = WordUtils.capitalizeFully(personinfo.navn, ' ', '-'),
                        dødsdato = null)
    }

    private fun finnVilkårsvurderingsperioder(behandlingId: UUID): Set<Vilkårsvurderingsperiode> {
        return vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.perioder ?: emptySet()
    }

    private fun finnVarsletDato(behandlingId: UUID): LocalDate? {
        return brevsporingService.finnSisteVarsel(behandlingId)?.sporbar?.opprettetTid?.toLocalDate()
    }

    private fun finnVarsletBeløp(behandling: Behandling): BigDecimal? {
        val varselbeløp = behandling.aktivtVarsel?.varselbeløp
        return if (varselbeløp == null) null else BigDecimal(varselbeløp)
    }

    private fun lagMetadataForVedtaksbrev(behandling: Behandling,
                                          fagsak: Fagsak,
                                          personinfo: Personinfo,
                                          vedtakResultatType: Vedtaksresultat?,
                                          brevmottager: Brevmottager): Brevmetadata {
        val språkkode: Språkkode = fagsak.bruker.språkkode
        val adresseinfo: Adresseinfo =
                eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem)
        val ytelsesnavn = fagsak.ytelsestype.navn[språkkode]!!
        val vergeNavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)
        val tilbakekreves = Vedtaksresultat.FULL_TILBAKEBETALING == vedtakResultatType ||
                            Vedtaksresultat.DELVIS_TILBAKEBETALING == vedtakResultatType
        val ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(behandling.ansvarligSaksbehandler)

        return Brevmetadata(ansvarligSaksbehandler = ansvarligSaksbehandler,
                            behandlendeEnhetId = behandling.behandlendeEnhet,
                            behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                            mottageradresse = adresseinfo,
                            ytelsestype = fagsak.ytelsestype,
                            saksnummer = fagsak.eksternFagsakId,
                            sakspartId = personinfo.ident,
                            sakspartsnavn = personinfo.navn,
                            språkkode = språkkode,
                            tittel = finnTittelVedtaksbrev(ytelsesnavn, tilbakekreves),
                            finnesVerge = behandling.harVerge,
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
        val aktsomhet = vilkårsvurdering?.aktsomhet
        val godTro = vilkårsvurdering?.godTro
        val beløpIBehold = if (godTro?.beløpErIBehold != false) godTro?.beløpTilbakekreves else BigDecimal.ZERO
        val aktsomhetsresultat = when {
            foreldelsePeriode?.erForeldet() == true -> AnnenVurdering.FORELDET
            godTro != null -> AnnenVurdering.GOD_TRO
            else -> aktsomhet?.aktsomhet
        }


        val unntasInnkrevingPgaLavtBeløp = aktsomhet?.tilbakekrevSmåbeløp == false
        val hbSærligeGrunner =
                if (aktsomhet != null && skalHaSærligeGrunner(aktsomhet.aktsomhet, unntasInnkrevingPgaLavtBeløp)) {
                    val særligeGrunner = aktsomhet.vilkårsvurderingSærligeGrunner
                            .map(VilkårsvurderingSærligGrunn::særligGrunn)
                    val fritekstSærligeGrunner = fritekst?.særligeGrunnerAvsnitt
                    val fritekstSærligGrunnAnnet = fritekst?.særligeGrunnerAnnetAvsnitt
                    HbSærligeGrunner(særligeGrunner, fritekstSærligeGrunner, fritekstSærligGrunnAnnet)
                } else {
                    null
                }


        return HbVurderinger(fritekst = fritekst?.vilkårAvsnitt,
                             vilkårsvurderingsresultat = vilkårsvurdering?.vilkårsvurderingsresultat,
                             unntasInnkrevingPgaLavtBeløp = aktsomhet?.tilbakekrevSmåbeløp == false,
                             særligeGrunner = hbSærligeGrunner,
                             aktsomhetsresultat = aktsomhetsresultat,
                             beløpIBehold = beløpIBehold,
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

    private fun skalHaSærligeGrunner(aktsomhet: Aktsomhet, unntattPgaLavgBeløp: Boolean): Boolean {
        return Aktsomhet.GROV_UAKTSOMHET == aktsomhet || Aktsomhet.SIMPEL_UAKTSOMHET == aktsomhet && !unntattPgaLavgBeløp
    }

    private fun finnForeldelsePeriode(foreldelse: VurdertForeldelse?, periode: Periode): Foreldelsesperiode? {
        return if (foreldelse == null) {
            null
        } else foreldelse.foreldelsesperioder
                       .firstOrNull { p -> p.periode.omslutter(periode) }
               ?: error("Fant ikke VurdertForeldelse-periode som omslutter periode $periode")
    }

    private fun hentFriteksterTilPerioder(behandlingId: UUID): List<PeriodeMedTekstDto> {
        val eksisterendePerioderForBrev: List<Vedtaksbrevsperiode> =
                vedtaksbrevsperiodeRepository.findByBehandlingId(behandlingId)
        return VedtaksbrevFritekstMapper.mapFritekstFraDb(eksisterendePerioderForBrev)
    }

    private fun hentOppsummeringFritekst(behandlingId: UUID): String? {
        val vedtaksbrevOppsummering: Vedtaksbrevsoppsummering? =
                vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        return vedtaksbrevOppsummering?.oppsummeringFritekst
    }

    private fun finnTittelVedtaksbrev(ytelsesnavn: String, tilbakekreves: Boolean): String {
        return if (tilbakekreves) {
            TITTEL_VEDTAK_TILBAKEBETALING + ytelsesnavn
        } else {
            TITTEL_VEDTAK_INGEN_TILBAKEBETALING + ytelsesnavn
        }
    }

    private inner class HbVedtaksResultatBeløp(resulatPerioder: List<Beregningsresultatsperiode>) {

        val totaltTilbakekrevesUtenRenter = resulatPerioder.sumOf { it.tilbakekrevingsbeløpUtenRenter }
        val totaltTilbakekrevesMedRenter = resulatPerioder.sumOf { it.tilbakekrevingsbeløp }
        val totaltRentebeløp = resulatPerioder.sumOf { it.rentebeløp }
        private val totaltSkattetrekk = resulatPerioder.sumOf { it.skattebeløp }
        val totaltTilbakekrevesBeløpMedRenterUtenSkatt: BigDecimal = totaltTilbakekrevesMedRenter.subtract(totaltSkattetrekk)
        val totaltFeilutbetaltBeløp = resulatPerioder.sumOf { it.feilutbetaltBeløp }

    }

    companion object {

        private const val TITTEL_VEDTAK_TILBAKEBETALING = "Vedtak tilbakebetaling "
        private const val TITTEL_VEDTAK_INGEN_TILBAKEBETALING = "Vedtak ingen tilbakebetaling "
        private const val KLAGEFRIST_UKER = 6
    }
}
