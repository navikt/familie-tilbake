package no.nav.tilbakekreving.e2e

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.matchers.shouldBe
import jakarta.jms.ConnectionFactory
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Duration.Companion.milliseconds

open class TilbakekrevingE2EBase : E2EBase() {
    @Autowired
    protected lateinit var kravgrunnlagBufferRepository: KravgrunnlagBufferRepository

    @Autowired
    protected lateinit var tilbakekrevingService: TilbakekrevingService

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Autowired
    protected lateinit var pdlClient: PdlClientMock

    @AfterEach
    fun reset() {
        pdlClient.reset()
    }

    fun sendMessage(
        queueName: String,
        text: String,
    ) {
        val connection = connectionFactory.createConnection()
        connection.createSession().use { session ->
            val message = session.createTextMessage(text)
            val queue = session.createQueue(queueName)
            session.createProducer(queue).use {
                it.send(message)
            }
        }
    }

    fun sendKravgrunnlagOgAvventLesing(
        queueName: String,
        kravgrunnlag: String,
    ) {
        sendMessage(queueName, kravgrunnlag)

        runBlocking {
            eventually(
                eventuallyConfig {
                    duration = 1000.milliseconds
                    interval = 10.milliseconds
                },
            ) {
                kravgrunnlagBufferRepository.hentUlesteKravgrunnlag().size shouldBe 1
            }
        }

        tilbakekrevingService.lesKravgrunnlag()

        kravgrunnlagBufferRepository.hentUlesteKravgrunnlag().size shouldBe 0
    }
}
