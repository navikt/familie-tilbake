package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.beregning.Kravgrunnlag431Adapter
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kontrakter.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.FeilutbetaltePerioderDto
import no.nav.tilbakekreving.kontrakter.ForhåndsvisVarselbrevRequest
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.FeilutbetaltPeriode
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Vedleggsdata
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class VarselbrevUtil(
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val oppdragClient: OppdragClient,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val organisasjonService: OrganisasjonService,
) {
    companion object {
        const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "
        const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    }

    fun sammenstillInfoForForhåndvisningVarselbrev(
        adresseinfo: Adresseinfo,
        request: ForhåndsvisVarselbrevRequest,
        personinfo: Personinfo,
    ): Varselbrevsdokument {
        val tittel = getTittelForVarselbrev(Ytelsestype.forDTO(request.ytelsestype).navn[request.språkkode]!!, false)
        val vergenavn = BrevmottagerUtil.getVergenavn(request.verge, adresseinfo)
        val ansvarligSaksbehandler =
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(
                ContextService.hentSaksbehandler(SecureLog.Context.tom()),
                SecureLog.Context.tom(),
            )

        val metadata =
            Brevmetadata(
                sakspartId = personinfo.ident,
                sakspartsnavn = personinfo.navn,
                finnesVerge = request.verge != null,
                vergenavn = vergenavn,
                mottageradresse = adresseinfo,
                behandlendeEnhetId = request.behandlendeEnhetId,
                behandlendeEnhetsNavn = request.behandlendeEnhetsNavn,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                saksnummer = request.eksternFagsakId,
                språkkode = request.språkkode,
                ytelsestype = request.ytelsestype,
                tittel = tittel,
                gjelderDødsfall = personinfo.dødsdato != null,
                institusjon =
                    request.institusjon?.let {
                        organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
                    },
            )

        return Varselbrevsdokument(
            brevmetadata = metadata,
            beløp = request.feilutbetaltePerioderDto.sumFeilutbetaling,
            revurderingsvedtaksdato = request.vedtaksdato ?: LocalDate.now(),
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = request.varseltekst,
            feilutbetaltePerioder = mapFeilutbetaltePerioder(request.feilutbetaltePerioderDto),
        )
    }

    fun sammenstillInfoForBrevmetadata(
        behandling: Behandling,
        personinfo: Personinfo,
        adresseinfo: Adresseinfo,
        fagsak: Fagsak,
        vergenavn: String?,
        erKorrigert: Boolean,
        gjelderDødsfall: Boolean,
        logContext: SecureLog.Context,
    ): Brevmetadata {
        val ansvarligSaksbehandler =
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(
                behandling.ansvarligSaksbehandler,
                logContext,
            )

        return Brevmetadata(
            sakspartId = personinfo.ident,
            sakspartsnavn = personinfo.navn,
            finnesVerge = behandling.harVerge,
            vergenavn = vergenavn,
            mottageradresse = adresseinfo,
            behandlendeEnhetId = behandling.behandlendeEnhet,
            behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            saksnummer = fagsak.eksternFagsakId,
            språkkode = fagsak.bruker.språkkode,
            ytelsestype = fagsak.ytelsestype.tilDTO(),
            tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert),
            gjelderDødsfall = gjelderDødsfall,
            institusjon =
                fagsak.institusjon?.let {
                    organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
                },
        )
    }

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(
        metadata: Brevmetadata,
        fritekst: String?,
        feilutbetalingsfakta: FaktaFeilutbetalingDto,
        varsel: Varsel?,
    ): Varselbrevsdokument =
        Varselbrevsdokument(
            brevmetadata = metadata,
            beløp = feilutbetalingsfakta.totaltFeilutbetaltBeløp.toLong(),
            revurderingsvedtaksdato = feilutbetalingsfakta.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = fritekst,
            feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingsfakta),
            erKorrigert = varsel != null,
            varsletDato = varsel?.sporbar?.opprettetTid?.toLocalDate(),
            varsletBeløp = varsel?.varselbeløp,
        )

    private fun sammenstillInfoFraSimuleringForVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        eksternBehandlingId: String,
        varsletTotalbeløp: Long,
        logContext: SecureLog.Context,
    ): Vedleggsdata {
        val request =
            HentFeilutbetalingerFraSimuleringRequest(
                varselbrevsdokument.ytelsestype,
                varselbrevsdokument.brevmetadata.saksnummer,
                eksternBehandlingId,
            )

        val feilutbetalingerFraSimulering = oppdragClient.hentFeilutbetalingerFraSimulering(request, logContext)

        val perioder =
            feilutbetalingerFraSimulering.feilutbetaltePerioder.map {
                FeilutbetaltPeriode(
                    YearMonth.from(it.fom),
                    it.nyttBeløp,
                    it.tidligereUtbetaltBeløp,
                    it.feilutbetaltBeløp,
                )
            }

        validerKorrektTotalbeløp(
            perioder,
            varsletTotalbeløp,
            Ytelsestype.forDTO(varselbrevsdokument.ytelsestype),
            varselbrevsdokument.brevmetadata.saksnummer,
            eksternBehandlingId,
            SecureLog.Context.utenBehandling(eksternBehandlingId),
        )
        return Vedleggsdata(varselbrevsdokument.språkkode, varselbrevsdokument.isYtelseMedSkatt, perioder)
    }

    private fun sammenstillInfoFraKravgrunnlag(
        varselbrevsdokument: Varselbrevsdokument,
        behandlingId: UUID,
    ): Vedleggsdata {
        val kravgrunnlag = Kravgrunnlag431Adapter(kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId))

        val perioder = kravgrunnlag.perioder().map {
            FeilutbetaltPeriode(
                YearMonth.from(it.periode().fom),
                it.beløpTilbakekreves().sumOf { beløp -> beløp.riktigYteslesbeløp() },
                it.beløpTilbakekreves().sumOf { beløp -> beløp.utbetaltYtelsesbeløp() },
                it.feilutbetaltYtelsesbeløp(),
            )
        }

        return Vedleggsdata(varselbrevsdokument.språkkode, varselbrevsdokument.isYtelseMedSkatt, perioder)
    }

    fun lagVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        fagsystemsbehandlingId: String?,
        varsletTotalbeløp: Long,
        logContext: SecureLog.Context,
    ): String =
        if (varselbrevsdokument.harVedlegg) {
            if (fagsystemsbehandlingId == null) {
                error(
                    "fagsystemsbehandlingId mangler for forhåndsvisning av varselbrev. " +
                        "Saksnummer ${varselbrevsdokument.brevmetadata.saksnummer}",
                )
            }

            val vedleggsdata =
                sammenstillInfoFraSimuleringForVedlegg(varselbrevsdokument, fagsystemsbehandlingId, varsletTotalbeløp, logContext)
            TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)
        } else {
            ""
        }

    fun lagVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        behandlingId: UUID,
    ): String =
        if (varselbrevsdokument.harVedlegg) {
            val vedleggsdata = sammenstillInfoFraKravgrunnlag(varselbrevsdokument, behandlingId)
            TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)
        } else {
            ""
        }

    private fun validerKorrektTotalbeløp(
        feilutbetaltePerioder: List<FeilutbetaltPeriode>,
        varsletTotalFeilutbetaltBeløp: Long,
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
        eksternId: String,
        logContext: SecureLog.Context,
    ) {
        if (feilutbetaltePerioder.sumOf { it.feilutbetaltBeløp.toLong() } != varsletTotalFeilutbetaltBeløp) {
            throw Feil(
                message =
                    "Varslet totalFeilutbetaltBeløp matcher ikke med hentet totalFeilutbetaltBeløp fra " +
                        "simulering for ytelsestype=$ytelsestype, eksternFagsakId=$eksternFagsakId og eksternId=$eksternId",
                logContext = logContext,
            )
        }
    }

    private fun getTittelForVarselbrev(
        ytelsesnavn: String,
        erKorrigert: Boolean,
    ): String =
        if (erKorrigert) {
            TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelsesnavn
        } else {
            TITTEL_VARSEL_TILBAKEBETALING + ytelsesnavn
        }

    private fun mapFeilutbetaltePerioder(feilutbetaltePerioderDto: FeilutbetaltePerioderDto): List<Datoperiode> =
        feilutbetaltePerioderDto.perioder.map {
            Datoperiode(
                it.fom,
                it.tom,
            )
        }

    private fun mapFeilutbetaltePerioder(feilutbetalingsfakta: FaktaFeilutbetalingDto): List<Datoperiode> =
        feilutbetalingsfakta.feilutbetaltePerioder.map {
            Datoperiode(
                it.periode.fom,
                it.periode.tom,
            )
        }
}
