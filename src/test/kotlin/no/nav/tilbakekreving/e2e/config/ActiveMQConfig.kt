package no.nav.tilbakekreving.e2e.config

import jakarta.jms.ConnectionFactory
import org.apache.activemq.ActiveMQConnectionFactory
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.JmsTransactionManager
import org.testcontainers.activemq.ActiveMQContainer

@TestConfiguration
class ActiveMQConfig {
    val activeMq = ActiveMQContainer("apache/activemq-classic:6.1.6")
        .withUser("srvtilbake")
        .withPassword("hunter2")
        .apply {
            start()
        }

    @Primary
    @Bean
    fun connectionFactory(): ConnectionFactory = ActiveMQConnectionFactory("srvtilbake", "hunter2", activeMq.brokerUrl)

    @Primary
    @Bean("jmsListenerContainerFactory")
    fun jmsListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        configurer: DefaultJmsListenerContainerFactoryConfigurer,
    ): JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, connectionFactory)

        val transactionManager = JmsTransactionManager()
        transactionManager.connectionFactory = connectionFactory
        factory.setTransactionManager(transactionManager)
        factory.setSessionTransacted(true)
        return factory
    }
}
