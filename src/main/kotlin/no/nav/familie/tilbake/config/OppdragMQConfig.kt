package no.nav.familie.tilbake.config

import com.ibm.mq.constants.CMQC
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.common.CommonConstants
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.JmsTransactionManager
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import javax.jms.ConnectionFactory
import javax.jms.JMSException

private const val UTF_8_WITH_PUA = 1208

@Configuration
class OppdragMQConfig(@Value("\${oppdrag.mq.hostname}") val hostname: String,
                      @Value("\${oppdrag.mq.queuemanager}") val queuemanager: String,
                      @Value("\${oppdrag.mq.channel}") val channel: String,
                      @Value("\${oppdrag.mq.port}") val port: Int,
                      @Value("\${oppdrag.mq.user}") val user: String,
                      @Value("\${oppdrag.mq.password}") val password: String,
                      val environment: Environment) {

    @Bean
    @Throws(JMSException::class)
    fun mqQueueConnectionFactory(): ConnectionFactory {
        val targetFactory = MQQueueConnectionFactory()
        targetFactory.hostName = hostname
        targetFactory.queueManager = queuemanager
        targetFactory.channel = channel
        targetFactory.port = port
        targetFactory.transportType = CommonConstants.WMQ_CM_CLIENT
        targetFactory.ccsid = UTF_8_WITH_PUA
        targetFactory.setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_NATIVE)
        targetFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, false)
        targetFactory.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)

        val cf = UserCredentialsConnectionFactoryAdapter()
        cf.setUsername(user)
        cf.setPassword(password)
        cf.setTargetConnectionFactory(targetFactory)
        return cf
    }


    @Bean
    fun jmsListenerContainerFactory(@Qualifier("mqQueueConnectionFactory") connectionFactory: ConnectionFactory,
                                    configurer: DefaultJmsListenerContainerFactoryConfigurer): JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, connectionFactory)

        val transactionManager = JmsTransactionManager()
        transactionManager.connectionFactory = connectionFactory
        factory.setTransactionManager(transactionManager)
        factory.setSessionTransacted(true)
        if (environment.activeProfiles.any { it.contains("local") }) {
            factory.setAutoStartup(false)
        }
        return factory
    }
}
