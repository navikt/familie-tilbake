package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ForhåndsvisVarselbrevRequest
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VarselbrevService(
    private val fagsakRepository: FagsakRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil,
    private val distribusjonshåndteringService: DistribusjonshåndteringService,
) {
    fun sendVarselbrev(
        behandling: Behandling,
        brevmottager: Brevmottager? = null,
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val varsletFeilutbetaling = behandling.aktivtVarsel?.varselbeløp ?: 0L
        val fritekst = behandling.aktivtVarsel?.varseltekst

        if (brevmottager == null) {
            distribusjonshåndteringService.sendBrev(behandling, Brevtype.VARSEL, varsletFeilutbetaling, fritekst) { brevmottaker, brevmetadata ->
                val varselbrevsdokument = lagVarselbrevForSending(behandling, fagsak, brevmottaker, brevmetadata, logContext)
                val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
                val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)

                val vedlegg =
                    varselbrevUtil.lagVedlegg(
                        varselbrevsdokument,
                        behandling.aktivFagsystemsbehandling.eksternId,
                        varselbrevsdokument.beløp,
                        logContext,
                    )
                Brevdata(
                    mottager = brevmottaker,
                    metadata = varselbrevsdokument.brevmetadata,
                    overskrift = overskrift,
                    brevtekst = brevtekst,
                    vedleggHtml = vedlegg,
                )
            }
        } else {
            val varselbrevsdokument = lagVarselbrevForSending(behandling, fagsak, brevmottager, null, logContext)
            val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
            val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
            val varsletFeilutbetaling = varselbrevsdokument.beløp
            val fritekst = varselbrevsdokument.varseltekstFraSaksbehandler
            val vedlegg =
                varselbrevUtil.lagVedlegg(
                    varselbrevsdokument,
                    behandling.aktivFagsystemsbehandling.eksternId,
                    varselbrevsdokument.beløp,
                    logContext,
                )

            pdfBrevService.sendBrev(
                behandling,
                fagsak,
                Brevtype.VARSEL,
                Brevdata(
                    mottager = brevmottager,
                    metadata = varselbrevsdokument.brevmetadata,
                    overskrift = overskrift,
                    brevtekst = brevtekst,
                    vedleggHtml = vedlegg,
                ),
                varsletFeilutbetaling,
                fritekst,
            )
        }
    }

    private fun lagVarselbrevForSending(
        behandling: Behandling,
        fagsak: Fagsak,
        brevmottager: Brevmottager,
        forhåndsgenerertMetadata: Brevmetadata?,
        logContext: SecureLog.Context,
    ): Varselbrevsdokument {
        val metadata =
            forhåndsgenerertMetadata ?: run {
                // Henter data fra pdl
                val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem, logContext)
                val verge = behandling.aktivVerge
                val adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, verge, fagsak.fagsystem, logContext)

                varselbrevUtil.sammenstillInfoForBrevmetadata(
                    behandling = behandling,
                    personinfo = personinfo,
                    adresseinfo = adresseinfo,
                    fagsak = fagsak,
                    vergenavn = BrevmottagerUtil.getVergenavn(verge, adresseinfo),
                    erKorrigert = false,
                    gjelderDødsfall = personinfo.dødsdato != null,
                    logContext = logContext,
                )
            }

        val varsel = behandling.aktivtVarsel

        return Varselbrevsdokument(
            brevmetadata = metadata.copy(tittel = TITTEL_VARSEL_TILBAKEBETALING + fagsak.ytelsesnavn),
            beløp = varsel?.varselbeløp ?: 0L,
            revurderingsvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = varsel?.varseltekst,
            feilutbetaltePerioder = mapFeilutbetaltePerioder(varsel),
        )
    }

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val varselbrevsdokument = lagVarselbrevForForhåndsvisning(forhåndsvisVarselbrevRequest)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val data =
            Fritekstbrevsdata(
                overskrift = overskrift,
                brevtekst = brevtekst,
                brevmetadata = varselbrevsdokument.brevmetadata,
            )
        val brevmottager = utledBrevmottager(forhåndsvisVarselbrevRequest)
        val vedlegg =
            varselbrevUtil.lagVedlegg(
                varselbrevsdokument,
                forhåndsvisVarselbrevRequest.fagsystemsbehandlingId,
                varselbrevsdokument.beløp,
                SecureLog.Context.utenBehandling(forhåndsvisVarselbrevRequest.eksternFagsakId),
            )
        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                metadata = data.brevmetadata,
                overskrift = data.overskrift,
                brevtekst = data.brevtekst,
                vedleggHtml = vedlegg,
            ),
        )
    }

    private fun lagVarselbrevForForhåndsvisning(request: ForhåndsvisVarselbrevRequest): Varselbrevsdokument {
        val brevmottager = utledBrevmottager(request)
        val personinfo = eksterneDataForBrevService.hentPerson(request.ident, Fagsystem.forDTO(request.fagsystem), SecureLog.Context.tom())
        val adresseinfo = eksterneDataForBrevService.hentAdresse(
            personinfo,
            brevmottager,
            request.verge,
            Fagsystem.forDTO(request.fagsystem),
            SecureLog.Context.tom(),
        )

        return varselbrevUtil.sammenstillInfoForForhåndvisningVarselbrev(
            adresseinfo,
            request,
            personinfo,
        )
    }

    private fun utledBrevmottager(request: ForhåndsvisVarselbrevRequest): Brevmottager =
        if (request.verge != null) {
            Brevmottager.VERGE
        } else if (request.institusjon != null) {
            Brevmottager.INSTITUSJON
        } else {
            Brevmottager.BRUKER
        }

    private fun mapFeilutbetaltePerioder(varsel: Varsel?): List<Datoperiode> =
        varsel?.perioder?.map {
            Datoperiode(
                it.fom,
                it.tom,
            )
        } ?: emptyList()
}
