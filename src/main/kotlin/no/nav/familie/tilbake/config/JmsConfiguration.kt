package no.nav.familie.tilbake.config

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagListener
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.JmsListenerConfigurer
import org.springframework.jms.config.JmsListenerEndpointRegistrar
import org.springframework.jms.config.SimpleJmsListenerEndpoint

@Configuration
class JmsConfiguration(
    private val applicationProperties: ApplicationProperties,
    private val kravgrunnlagListener: KravgrunnlagListener,
) : JmsListenerConfigurer {
    override fun configureJmsListeners(registrar: JmsListenerEndpointRegistrar) {
        applicationProperties.kravgrunnlag.forEach {
            registrar.registerEndpoint(
                SimpleJmsListenerEndpoint().apply {
                    id = "kravgrunnlag_$it"
                    destination = it
                    messageListener = kravgrunnlagListener
                },
            )
        }
    }
}
