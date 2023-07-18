package no.nav.familie.tilbake.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import no.nav.familie.kafka.KafkaErrorHandler
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.listener.MessageListenerContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaErrorHandlerTest {

    @MockK(relaxed = true)
    lateinit var container: MessageListenerContainer

    @MockK(relaxed = true)
    lateinit var consumer: Consumer<*, *>

    @InjectMockKs
    lateinit var errorHandler: KafkaErrorHandler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        shouldThrow<RuntimeException> { errorHandler.handleRemaining(RuntimeException("Feil i test"), emptyList(), consumer, container) }
            .message shouldNotContain "Feil i test" shouldContain "Sjekk securelogs for mer info"
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")
        shouldThrow<RuntimeException> {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                listOf(consumerRecord),
                consumer,
                container
            )
        }.message shouldNotContain "Feil i test" shouldContain "Sjekk securelogs for mer info"
    }
}
