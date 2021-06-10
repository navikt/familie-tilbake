package no.nav.familie.tilbake.config

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.ext.logging.LoggingInInterceptor
import org.apache.cxf.ext.logging.LoggingOutInterceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.xml.namespace.QName

@Configuration
@Profile("!local")
class ØkonomiConsumerConfig(@Value("\${TILBAKEKREVING_V1_URL}") private val tilbakekrevingUrl: String) {

    private val WSDL = "wsdl/no/nav/tilbakekreving/tilbakekreving-v1-tjenestespesifikasjon.wsdl"
    private val NAMESPACE = "http://okonomi.nav.no/tilbakekrevingService/"
    private val SERVICE = QName(NAMESPACE, "TilbakekrevingService")
    private val PORT = QName(NAMESPACE, "TilbakekrevingServicePort")

    @Bean
    fun økonomiService(): TilbakekrevingPortType {
        val factoryBean = JaxWsProxyFactoryBean().apply {
            wsdlURL = WSDL
            serviceName = SERVICE
            endpointName = PORT
            serviceClass = TilbakekrevingPortType::class.java
            address = tilbakekrevingUrl
            features.add(WSAddressingFeature())
            features.add(loggingFeature())
            outInterceptors.add(LoggingOutInterceptor())
            inInterceptors.add(LoggingInInterceptor())
        }
        return factoryBean.create(TilbakekrevingPortType::class.java)
    }

    private fun loggingFeature(): LoggingFeature {
        val loggingFeature = LoggingFeature()
        loggingFeature.setPrettyLogging(true)
        loggingFeature.setVerbose(true)
        return loggingFeature
    }
}
