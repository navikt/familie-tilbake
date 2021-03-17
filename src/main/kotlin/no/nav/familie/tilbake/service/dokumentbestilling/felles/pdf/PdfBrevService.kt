package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import no.nav.familie.tilbake.service.pdfgen.Dokumentvariant
import no.nav.familie.tilbake.service.pdfgen.PdfGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdfBrevService(private val journalføringService: JournalføringService) {

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
        valider(brevtype, data)
        val dokumentreferanse: JournalpostIdOgDokumentId = lagOgJournalførBrev(behandling, fagsak, brevtype, data)
//        TODO opprettes task
        //        lagTaskerForUtsendingOgSporing(behandlingId, brevtype, varsletBeløp, fritekst, data, dokumentreferanse)
    }

    private fun lagOgJournalførBrev(behandling: Behandling,
                                    fagsak: Fagsak,
                                    brevtype: Brevtype,
                                    data: Brevdata): JournalpostIdOgDokumentId {
        return JournalpostIdOgDokumentId("dummy")
        //  TODO Kommenteres inn når familie integrasjoner er tilpasset
//        val html = lagHtml(data)
//        val pdf: ByteArray = pdfGenerator.genererPDFMedLogo(html, DokumentVariant.ENDELIG)

        //        return journalføringService.journalførUtgåendeBrev(behandling,
//                                                            fagsak,
//                                                            mapBrevTypeTilDokumentKategori(brevtype),
//                                                            data.metadata,
//                                                            data.mottaker,
//                                                            pdf)
    }


    private fun mapBrevtypeTilDokumentkategori(brevType: Brevtype): Dokumentkategori {
        return if (Brevtype.VEDTAK === brevType) {
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

    private fun lagHeader(data: Brevdata): String? {
        return TekstformatererHeader.lagHeader(data.metadata, data.overskrift)
    }

    companion object {

        private fun valider(brevType: Brevtype, varsletBeløp: Long?) {
            val harVarsletBeløp = varsletBeløp != null
            require(brevType.gjelderVarsel() == harVarsletBeløp) {
                "Utvikler-feil: Varslet beløp skal brukes hvis, og bare hvis, brev gjelder varsel"
            }
        }

        private fun valider(brevType: Brevtype, data: Brevdata) {
            require(!(brevType == Brevtype.FRITEKST && data.tittel == null)) {
                "Utvikler-feil: For brevType = $brevType må tittel være satt"
            }
        }
    }
}
