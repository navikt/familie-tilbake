package no.nav.tilbakekreving.e2e

import jakarta.jms.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired

class TilbakekrevingE2EBase : E2EBase() {
    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

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
}
