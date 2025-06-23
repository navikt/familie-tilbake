package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.expectSingleOrNull
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.Avsnitt
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.AvsnittUtil
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.TekstformatererVedtaksbrev
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VedtaksbrevService(
    private val behandlingRepository: BehandlingRepository,
    private val vedtaksbrevgeneratorService: VedtaksbrevgeneratorService,
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService,
    private val faktaRepository: FaktaFeilutbetalingRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
    private val vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository,
    private val pdfBrevService: PdfBrevService,
    private val distribusjonshåndteringService: DistribusjonshåndteringService,
    private val periodeService: PeriodeService,
    private val logService: LogService,
) {
    fun sendVedtaksbrev(
        behandling: Behandling,
    ) {
        val vedtaksbrevgrunnlag = vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(behandling.id)
        val logContext = logService.contextFraBehandling(behandling.id)

        distribusjonshåndteringService.sendBrev(behandling, Brevtype.VEDTAK) { brevmottaker, brevmetadata ->
            vedtaksbrevgeneratorService.genererVedtaksbrevForSending(vedtaksbrevgrunnlag, brevmottaker, brevmetadata, logContext)
        }
    }

    fun hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(hentForhåndvisningVedtaksbrevPdfDto: HentForhåndvisningVedtaksbrevPdfDto): ByteArray {
        val logContext = logService.contextFraBehandling(hentForhåndvisningVedtaksbrevPdfDto.behandlingId)
        val vedtaksbrevgrunnlag = vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(hentForhåndvisningVedtaksbrevPdfDto.behandlingId)
        val brevdata = vedtaksbrevgeneratorService.genererVedtaksbrevForForhåndsvisning(vedtaksbrevgrunnlag, hentForhåndvisningVedtaksbrevPdfDto, logContext)

        return pdfBrevService.genererForhåndsvisning(brevdata)
    }

    fun hentVedtaksbrevSomTekst(
        behandlingId: UUID,
    ): List<Avsnitt> {
        val logContext = logService.contextFraBehandling(behandlingId)
        val vedtaksbrevgrunnlag = vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(behandlingId)
        val skalSammenslåPerioder = periodeService.erPerioderSammenslått(behandlingId)
        val hbVedtaksbrevsdata = vedtaksbrevgeneratorService.genererVedtaksbrevsdataTilVisningIFrontendSkjema(vedtaksbrevgrunnlag, logContext)
        val hovedoverskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(hbVedtaksbrevsdata)
        return AvsnittUtil.lagVedtaksbrevDeltIAvsnitt(hbVedtaksbrevsdata, hovedoverskrift, skalSammenslåPerioder)
    }

    @Transactional
    fun lagreUtkastAvFritekster(
        behandlingId: UUID,
        fritekstavsnittDto: FritekstavsnittDto,
        logContext: SecureLog.Context,
    ) {
        lagreFriteksterFraSaksbehandler(behandlingId, fritekstavsnittDto, false, logContext)
    }

    @Transactional
    fun lagreFriteksterFraSaksbehandler(
        behandlingId: UUID,
        fritekstavsnittDto: FritekstavsnittDto,
        logContext: SecureLog.Context,
    ) {
        lagreFriteksterFraSaksbehandler(behandlingId, fritekstavsnittDto, true, logContext)
    }

    private fun lagreFriteksterFraSaksbehandler(
        behandlingId: UUID,
        fritekstavsnittDto: FritekstavsnittDto,
        validerPåkrevetFritekster: Boolean = false,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val vedtaksbrevstype = behandling.utledVedtaksbrevstype()
        val skalSammenslåPerioder = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)?.skalSammenslåPerioder ?: SkalSammenslåPerioder.IKKE_AKTUELT
        val vedtaksbrevsoppsummering = VedtaksbrevFritekstMapper.tilDomene(behandlingId, fritekstavsnittDto.oppsummeringstekst, skalSammenslåPerioder)

        val vedtaksbrevsperioder = VedtaksbrevFritekstMapper.tilDomeneVedtaksbrevsperiode(behandlingId, fritekstavsnittDto.perioderMedTekst)

        // Valider om obligatoriske fritekster er satt
        val faktaFeilutbetaling = faktaRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }

        VedtaksbrevFritekstValidator.validerObligatoriskeFritekster(
            behandling = behandling,
            faktaFeilutbetaling = faktaFeilutbetaling,
            vilkårsvurdering = vilkårsvurdering,
            vedtaksbrevFritekstPerioder = vedtaksbrevsperioder,
            avsnittMedPerioder = fritekstavsnittDto.perioderMedTekst,
            vedtaksbrevsoppsummering = vedtaksbrevsoppsummering,
            vedtaksbrevstype = vedtaksbrevstype,
            validerPåkrevetFritekster = validerPåkrevetFritekster,
            logContext = logContext,
        )
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
        vedtaksbrevsoppsummeringRepository
            .findByBehandlingId(behandlingId)
            ?.let { vedtaksbrevsoppsummeringRepository.deleteById(it.id) }
        vedtaksbrevsperiodeRepository.findByBehandlingId(behandlingId).forEach { vedtaksbrevsperiodeRepository.deleteById(it.id) }
    }

    @Transactional
    fun oppdaterSkalSammenslåPerioder(
        behandlingId: UUID,
        skalSammenslåPerioder: SkalSammenslåPerioder,
    ) {
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)
        if (vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = skalSammenslåPerioder))
        } else {
            vedtaksbrevsoppsummeringRepository.insert(Vedtaksbrevsoppsummering(behandlingId = behandlingId, oppsummeringFritekst = "", skalSammenslåPerioder = skalSammenslåPerioder))
        }
    }
}
