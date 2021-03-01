package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import no.nav.familie.tilbake.service.pdfgen.DokumentVariant
import no.nav.familie.tilbake.service.pdfgen.PdfGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdfBrevTjeneste(private val journalføringTjeneste: JournalføringTjeneste) {

    private val logger = LoggerFactory.getLogger(PdfBrevTjeneste::class.java)
    private val pdfGenerator: PdfGenerator = PdfGenerator()

    fun genererForhåndsvisning(data: BrevData): ByteArray {
        val html = lagHtml(data)
        return pdfGenerator.genererPDFMedLogo(html, DokumentVariant.UTKAST)
    }

    fun sendBrev(behandling: Behandling,
                 fagsak: Fagsak,
                 brevtype: Brevtype,
                 data: BrevData,
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
                                    data: BrevData): JournalpostIdOgDokumentId {
        return JournalpostIdOgDokumentId("dummy")
        //  TODO Kommenteres inn når familie integrasjoner er tilpasset
//        val html = lagHtml(data)
//        val pdf: ByteArray = pdfGenerator.genererPDFMedLogo(html, DokumentVariant.ENDELIG)

        //        return journalføringTjeneste.journalførUtgåendeBrev(behandling,
//                                                            fagsak,
//                                                            mapBrevTypeTilDokumentKategori(brevtype),
//                                                            data.metadata,
//                                                            data.mottaker,
//                                                            pdf)
    }


    private fun mapBrevTypeTilDokumentKategori(brevType: Brevtype): Dokumentkategori {
        return if (Brevtype.VEDTAK === brevType) {
            Dokumentkategori.VEDTAKSBREV
        } else {
            Dokumentkategori.BREV
        }
    }

    private fun lagHtml(data: BrevData): String {
        val header = lagHeader(data)
        val innholdHtml = lagInnhold(data)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagInnhold(data: BrevData): String {
        return DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
    }

    private fun lagHeader(data: BrevData): String? {
        return TekstformatererHeader.lagHeader(data.metadata, data.overskrift)
    }

    companion object {

        private fun valider(brevType: Brevtype, varsletBeløp: Long?) {
            val harVarsletBeløp = varsletBeløp != null
            require(brevType.gjelderVarsel() == harVarsletBeløp) {
                "Utvikler-feil: Varslet beløp skal brukes hvis, og bare hvis, brev gjelder varsel"
            }
        }

        private fun valider(brevType: Brevtype, data: BrevData) {
            require(!(brevType == Brevtype.FRITEKST && data.tittel == null)) {
                "Utvikler-feil: For brevType = $brevType må tittel være satt"
            }
        }
    }
}
