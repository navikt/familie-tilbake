package no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ManueltVarselbrevService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val varselbrevUtil: VarselbrevUtil
) {

    fun sendManueltVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager) {
        sendVarselBrev(behandling, fritekst, brevmottager, false)
    }

    fun sendKorrigertVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager) {
        sendVarselBrev(behandling, fritekst, brevmottager, true)
    }

    fun sendVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager, erKorrigert: Boolean) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevsdokument =
            lagVarselbrev(fritekst, behandling, fagsak, brevmottager, erKorrigert, behandling.aktivtVarsel)
        val overskrift =
            TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, erKorrigert)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, erKorrigert)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
        val vedlegg = varselbrevUtil.lagVedlegg(varselbrevsdokument, behandling.id)
        pdfBrevService.sendBrev(
            behandling,
            fagsak,
            if (erKorrigert) Brevtype.KORRIGERT_VARSEL else Brevtype.VARSEL,
            Brevdata(
                mottager = brevmottager,
                overskrift = overskrift,
                brevtekst = brevtekst,
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = vedlegg
            ),
            varsletFeilutbetaling,
            fritekst
        )
    }

    fun hentForhåndsvisningManueltVarselbrev(
        behandlingId: UUID,
        maltype: Dokumentmalstype,
        fritekst: String
    ): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevmottager = BrevmottagerUtil.utledBrevmottager(behandling, fagsak)
        val erKorrigert = maltype == Dokumentmalstype.KORRIGERT_VARSEL

        val varselbrevsdokument =
            lagVarselbrev(fritekst, behandling, fagsak, brevmottager, erKorrigert, behandling.aktivtVarsel)
        val overskrift =
            TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, erKorrigert)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, erKorrigert)
        val vedlegg = varselbrevUtil.lagVedlegg(varselbrevsdokument, behandlingId)
        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                overskrift = overskrift,
                brevtekst = brevtekst,
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = vedlegg
            )
        )
    }

    private fun lagVarselbrev(
        fritekst: String,
        behandling: Behandling,
        fagsak: Fagsak,
        brevmottager: Brevmottager,
        erKorrigert: Boolean,
        aktivtVarsel: Varsel? = null
    ): Varselbrevsdokument {
        // Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
            eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem, behandling.id)
        val vergenavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)

        // Henter feilutbetaling fakta
        val feilutbetalingsfakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)

        val metadata = varselbrevUtil.sammenstillInfoForBrevmetadata(
            behandling,
            personinfo,
            adresseinfo,
            fagsak,
            vergenavn,
            erKorrigert,
            personinfo.dødsdato != null
        )

        return varselbrevUtil.sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(
            metadata,
            fritekst,
            feilutbetalingsfakta,
            aktivtVarsel
        )
    }
}
