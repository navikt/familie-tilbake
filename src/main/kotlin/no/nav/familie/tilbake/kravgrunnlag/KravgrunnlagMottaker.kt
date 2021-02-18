package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.jms.TextMessage

import no.nav.familie.prosessering.domene.TaskRepository
import java.util.*


@Service
@Profile("!e2e")
class KravgrunnlagMottaker(private val taskRepository: TaskRepository) {

    private val LOG = LoggerFactory.getLogger(KravgrunnlagMottaker::class.java)
    private val SECURE_LOGG = LoggerFactory.getLogger("secureLogger")

    @Transactional
    @JmsListener(destination = "\${oppdrag.mq.kravgrunnlag}", containerFactory = "jmsListenerContainerFactory")
    fun mottaKravgrunnlagFraOppdrag(melding: TextMessage) {
        val kravgrunnlagFraOppdrag = melding.text as String

        LOG.info("Mottatt kravgrunnlag fra oppdrag")
        SECURE_LOGG.info(kravgrunnlagFraOppdrag)

        taskRepository.save(
            Task(
                type = BehandleKravgrunnlagTask.BEHANDLE_KRAVGRUNNLAG,
                payload = kravgrunnlagFraOppdrag,
                properties = Properties().apply {
                    this["callId"] = UUID.randomUUID()
                })
        )

    }

}