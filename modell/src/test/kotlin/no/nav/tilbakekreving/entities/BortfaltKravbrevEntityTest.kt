package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.brev.BortfaltKravbrev
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.test.juni
import org.junit.jupiter.api.Test

class BortfaltKravbrevEntityTest {
    @Test
    fun `brev om bortfalt krav`() {
        val bortfaltKravbrev = BortfaltKravbrev.opprett(KlokkeStub(2.juni(2026)))
        bortfaltKravbrev.brevSendt(journalpostId = "bortfalt-jp", dokumentInfoId = "bortfalt-dok")

        val gjenopprettet = bortfaltKravbrev.tilEntity("tilbakekreving-id").fraEntity(KravgrunnlagHistorikk(mutableListOf()))

        gjenopprettet shouldBe bortfaltKravbrev
    }
}
