package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.brev.Informasjonsbrev
import no.nav.tilbakekreving.brev.Informasjonsbrevtype
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.test.juni
import org.junit.jupiter.api.Test

class InformasjonsbrevEntityTest {
    @Test
    fun `informasjonsbrev om bortfalt krav`() {
        val informasjonsbrev = Informasjonsbrev.opprett(Informasjonsbrevtype.BORTFALT_KRAV, KlokkeStub(2.juni(2026)))
        informasjonsbrev.brevSendt(journalpostId = "informasjonsbrev-jp", dokumentInfoId = "informasjonsbrev-dok")

        val gjenopprettet = informasjonsbrev.tilEntity("tilbakekreving-id").fraEntity(KravgrunnlagHistorikk(mutableListOf()))

        gjenopprettet shouldBe informasjonsbrev
    }
}
