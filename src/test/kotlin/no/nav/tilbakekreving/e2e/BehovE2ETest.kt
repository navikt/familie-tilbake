package no.nav.tilbakekreving.e2e

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.api.v2.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehovE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    @Test
    fun `sender behov om fagsysteminfo på kafka`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemBehandling = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                referanse = fagsystemBehandling,
            ),
        )

        val hendelser = kafkaProducer.finnKafkamelding(fagsystemId)
        hendelser.size shouldBe 1
        val fagsysteminfoBehov = hendelser.single { it.hendelsestype == "fagsysteminfo_behov" }
            .shouldBeInstanceOf<FagsysteminfoBehovHendelse>()

        fagsysteminfoBehov.eksternFagsakId shouldBe fagsystemId
        fagsysteminfoBehov.kravgrunnlagReferanse shouldBe fagsystemBehandling
    }
}
