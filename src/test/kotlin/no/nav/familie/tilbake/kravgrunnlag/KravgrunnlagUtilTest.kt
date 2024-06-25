package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeEmpty
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.junit.jupiter.api.Test
import java.math.BigInteger

class KravgrunnlagUtilTest {
    @Test
    fun `skal ikke finne diff hvis omgjortVedtakId er null og 0`() {
        val detaljertKravgrunnlag0Verdi: DetaljertKravgrunnlagDto =
            KravgrunnlagUtil
                .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        detaljertKravgrunnlag0Verdi.vedtakIdOmgjort = BigInteger.ZERO

        val detaljertKravgrunnlagNullVerdi =
            KravgrunnlagUtil
                .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        detaljertKravgrunnlagNullVerdi.vedtakIdOmgjort = null

        val diff = KravgrunnlagUtil.sammenlignKravgrunnlag(detaljertKravgrunnlagNullVerdi, detaljertKravgrunnlag0Verdi)
        diff.shouldBeEmpty()
    }

    @Test
    fun `skal finne diff hvis omgjortVedtakId er null og 0`() {
        val detaljertKravgrunnlag0Verdi: DetaljertKravgrunnlagDto =
            KravgrunnlagUtil
                .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        detaljertKravgrunnlag0Verdi.vedtakIdOmgjort = BigInteger.ZERO

        val detaljertKravgrunnlagNullVerdi =
            KravgrunnlagUtil
                .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        detaljertKravgrunnlagNullVerdi.vedtakIdOmgjort = BigInteger.ONE

        val diff = KravgrunnlagUtil.sammenlignKravgrunnlag(detaljertKravgrunnlagNullVerdi, detaljertKravgrunnlag0Verdi)
        diff.shouldNotBeEmpty()
    }

    private fun readXml(fileName: String): String {
        val url = requireNotNull(this::class.java.getResource(fileName)) { "fil med filnavn=$fileName finnes ikke" }
        return url.readText()
    }
}
