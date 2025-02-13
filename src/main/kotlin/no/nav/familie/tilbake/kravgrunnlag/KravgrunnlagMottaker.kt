package no.nav.familie.tilbake.kravgrunnlag

import jakarta.jms.TextMessage
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
@Profile("!e2e & !integrasjonstest")
@ConditionalOnProperty(
    value = ["oppdrag.mq.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class KravgrunnlagMottaker(
    private val taskService: TracableTaskService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    @JmsListener(destination = "\${oppdrag.mq.kravgrunnlag}", containerFactory = "jmsListenerContainerFactory")
    fun mottaMeldingFraOppdrag(melding: TextMessage) {
        val meldingFraOppdrag = melding.text as String

        log.info("Mottatt melding fra oppdrag")
        if (meldingFraOppdrag.contains(Constants.KRAVGRUNNLAG_XML_ROOT_ELEMENT)) {
            val logContext = KravgrunnlagUtil.kravgrunnlagLogContext(meldingFraOppdrag)
            SecureLog.medContext(logContext) { info(meldingFraOppdrag) }
            taskService.save(
                Task(
                    type = BehandleKravgrunnlagTask.TYPE,
                    payload = meldingFraOppdrag,
                    properties =
                        Properties().apply {
                            this["callId"] = UUID.randomUUID()
                        },
                ),
                logContext,
            )
        } else {
            val logContext = KravgrunnlagUtil.statusmeldingLogContext(meldingFraOppdrag)
            SecureLog.medContext(logContext) { info(meldingFraOppdrag) }
            taskService.save(
                Task(
                    type = BehandleStatusmeldingTask.TYPE,
                    payload = meldingFraOppdrag,
                    properties =
                        Properties().apply {
                            this["callId"] = UUID.randomUUID()
                        },
                ),
                logContext,
            )
        }
    }
}
