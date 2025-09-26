package no.nav.familie.tilbake.integration.kafka

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.fagsystem.Ytelse
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class DefaultKafkaProducerTest {
    @Test
    fun `joins kafka melding og standardfelter i json`() {
        val hendelseOpprettet = LocalDateTime.of(2025, 9, 26, 12, 30, 0)
        val properties = KafkaProperties(
            hentFagsystem = KafkaProperties.HentFagsystem("", ""),
        )
        val template = mockk<KafkaTemplate<String, String>>()
        val kafkaProducer = DefaultKafkaProducer(template, properties)

        val meldingSlot = slot<String>()

        every { template.send(any<String>(), any<String>(), capture(meldingSlot)) } returns CompletableFuture<SendResult<String, String>>().apply {
            complete(mockk())
        }

        kafkaProducer.sendKafkaEvent(
            FagsysteminfoBehovHendelse(
                eksternFagsakId = "123456",
                kravgrunnlagReferanse = "654321",
                hendelseOpprettet = hendelseOpprettet,
            ),
            FagsysteminfoBehovHendelse.METADATA,
            vedtakGjelderId = "20046954321",
            ytelse = Ytelse.Tilleggsstønad,
            logContext = SecureLog.Context.tom(),
        )

        val melding = objectMapper.readTree(meldingSlot.captured)
        melding["hendelsestype"]?.asText() shouldBe "fagsysteminfo_behov"
        melding["versjon"]?.asInt() shouldBe 1
        melding["hendelseOpprettet"]?.asText() shouldBe "2025-09-26T12:30:00"
        melding["eksternFagsakId"]?.asText() shouldBe "123456"

        // Spesifikke felter
        melding["kravgrunnlagReferanse"]?.asText() shouldBe "654321"
    }
}
