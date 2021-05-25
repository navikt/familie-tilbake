package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.familie.tilbake.service.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import no.nav.familie.tilbake.service.pdfgen.Dokumentvariant
import no.nav.familie.tilbake.service.pdfgen.PdfGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class PdfBrevService(private val journalføringService: JournalføringService,
                     private val taskService: TaskService) {

    private val logger = LoggerFactory.getLogger(PdfBrevService::class.java)
    private val pdfGenerator: PdfGenerator = PdfGenerator()

    fun genererForhåndsvisning(data: Brevdata): ByteArray {
        val html = lagHtml(data)
        return pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.UTKAST)
    }

    fun sendBrev(behandling: Behandling,
                 fagsak: Fagsak,
                 brevtype: Brevtype,
                 data: Brevdata,
                 varsletBeløp: Long? = null,
                 fritekst: String? = null) {
        valider(brevtype, varsletBeløp)
        val dokumentreferanse: JournalpostIdOgDokumentId = lagOgJournalførBrev(behandling, fagsak, brevtype, data)
        lagTaskerForUtsendingOgSporing(behandling, fagsak, brevtype, varsletBeløp, fritekst, data, dokumentreferanse)
    }

    private fun lagTaskerForUtsendingOgSporing(behandling: Behandling,
                                               fagsak: Fagsak,
                                               brevtype: Brevtype,
                                               varsletBeløp: Long?,
                                               fritekst: String?,
                                               brevdata: Brevdata,
                                               dokumentreferanse: JournalpostIdOgDokumentId) {

        val idString = behandling.id.toString()
        val properties: Properties = Properties().apply {
            setProperty("journalpostId", dokumentreferanse.journalpostId)
            setProperty("fagsystem", fagsak.fagsystem.name)
            setProperty("dokumentId", dokumentreferanse.dokumentId)
            setProperty("mottager", brevdata.mottager.name)
            setProperty("brevtype", brevtype.name)
            setProperty("ansvarligSaksbehandler", brevdata.metadata.ansvarligSaksbehandler)
            varsletBeløp?.also { setProperty("varselbeløp", varsletBeløp.toString()) }
            fritekst?.also { setProperty("fritekst", fritekst) }
            brevdata.tittel?.also { setProperty("tittel", it) }
        }
        logger.info("Oppretter task for publisering av brev for behandlingId=${behandling.id}]")
        taskService.save(Task(PubliserJournalpostTask.TYPE, idString, properties))
    }

    private fun lagOgJournalførBrev(behandling: Behandling,
                                    fagsak: Fagsak,
                                    brevtype: Brevtype,
                                    data: Brevdata): JournalpostIdOgDokumentId {
        val html = lagHtml(data)
        val pdf: ByteArray = pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.ENDELIG)

        return journalføringService.journalførUtgåendeBrev(behandling,
                                                           fagsak,
                                                           mapBrevtypeTilDokumentkategori(brevtype),
                                                           data.metadata,
                                                           data.mottager,
                                                           pdf)
    }

    private fun mapBrevtypeTilDokumentkategori(brevtype: Brevtype): Dokumentkategori {
        return if (Brevtype.VEDTAK === brevtype) {
            Dokumentkategori.VEDTAKSBREV
        } else {
            Dokumentkategori.BREV
        }
    }

    private fun lagHtml(data: Brevdata): String {
        val header = lagHeader(data)
        val innholdHtml = lagInnhold(data)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagInnhold(data: Brevdata): String {
        return DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
    }

    private fun lagHeader(data: Brevdata): String {
        return TekstformatererHeader.lagHeader(data.metadata, data.overskrift)
    }

    companion object {

        private fun valider(brevtype: Brevtype, varsletBeløp: Long?) {
            val harVarsletBeløp = varsletBeløp != null
            require(brevtype.gjelderVarsel() == harVarsletBeløp) {
                "Utvikler-feil: Varslet beløp skal brukes hvis, og bare hvis, brev gjelder varsel"
            }
        }
    }
}
