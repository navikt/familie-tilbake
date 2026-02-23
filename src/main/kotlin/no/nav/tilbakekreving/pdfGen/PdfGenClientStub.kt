package no.nav.tilbakekreving.pdfGen

import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.tilbakekreving.integrasjoner.pdfGen.PdfGenClient

class PdfGenClientStub() : PdfGenClient {
    override fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        return ByteArray(0)
    }
}
