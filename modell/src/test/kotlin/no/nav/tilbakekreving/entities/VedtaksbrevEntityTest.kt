package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class VedtaksbrevEntityTest {
    @Test
    fun `ordinær vedtaksbrev blir lagret`() {
        val vedtaksbrevFørLagring = Vedtaksbrev(
            id = UUID.randomUUID(),
            journalpostId = "journalpostId",
            dokumentInfoId = "dokumentInfoId",
            sendtTid = LocalDate.now(),
            opprettet = LocalDateTime.now(),
            vedtaksbrevType = Vedtaksbrev.Vedtaksbrevtype.ORDINÆR,
        )

        val vedtaksbrevEtterLagring = vedtaksbrevFørLagring.tilEntity("no-needed").fraEntity(KravgrunnlagHistorikk(mutableListOf()))

        vedtaksbrevEtterLagring shouldBe vedtaksbrevFørLagring
    }

    @Test
    fun `revurdering vedtaksbrev blir lagret`() {
        val vedtaksbrevFørLagring = Vedtaksbrev(
            id = UUID.randomUUID(),
            journalpostId = "journalpostId",
            dokumentInfoId = "dokumentInfoId",
            sendtTid = LocalDate.now(),
            opprettet = LocalDateTime.now(),
            vedtaksbrevType = Vedtaksbrev.Vedtaksbrevtype.REVURDERING,
        )

        val vedtaksbrevEtterLagring = vedtaksbrevFørLagring.tilEntity("no-needed").fraEntity(KravgrunnlagHistorikk(mutableListOf()))

        vedtaksbrevEtterLagring shouldBe vedtaksbrevFørLagring
    }
}
