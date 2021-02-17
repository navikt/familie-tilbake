package no.nav.familie.tilbake.kravgrunnlag

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.jms.TextMessage

@Service
@Profile("!e2e")
class KravgrunnlagMottaker(
    val env: Environment
) {

    internal var LOG = LoggerFactory.getLogger(KravgrunnlagMottaker::class.java)
    private val SECURE_LOGG = LoggerFactory.getLogger("secureLogger")

    @Transactional
    @JmsListener(destination = "\${oppdrag.mq.kravgrunnlag}", containerFactory = "jmsListenerContainerFactory")
    fun mottaKravgrunnlagFraOppdrag(melding: TextMessage) {
        var kravgrunnlagFraOppdrag = melding.text as String

        LOG.info("Mottatt kravgrunnlag fra oppdrag")
        SECURE_LOGG.info(kravgrunnlagFraOppdrag)


    }

}