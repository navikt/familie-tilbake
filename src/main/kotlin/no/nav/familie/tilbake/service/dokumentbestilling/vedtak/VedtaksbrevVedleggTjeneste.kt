package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.tilbake.service.pdfgen.Dokumentvariant
import no.nav.familie.tilbake.service.pdfgen.PdfGenerator

class VedtaksbrevsvedleggService {

    private val pdfGenerator: PdfGenerator = PdfGenerator()
    fun lagVedlegg(data: Vedtaksbrevsdata, dokumentVariant: Dokumentvariant): ByteArray {
        val dokumentSomSteng = TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(data.vedtaksbrevsdata)
        return pdfGenerator.genererPDF(dokumentSomSteng, dokumentVariant)
    }
}
