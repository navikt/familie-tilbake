package no.nav.tilbakekreving.pdfGen

import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto

class PdfGenClientStub() : PdfGenClient {
    override fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        return ByteArray(0)
    }
}
