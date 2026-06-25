package no.nav.tilbakekreving.pdfGen

import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.kontrakter.frontend.models.VarselbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto

class PdfGenClientStub() : PdfGenClient {
    override fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        return ByteArray(0)
    }

    override fun hentPdfForForhåndsvarsel(varselbrevData: VarselbrevDataDto): ByteArray {
        return ByteArray(0)
    }
}
