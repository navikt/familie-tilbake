package no.nav.tilbakekreving.fagsystem

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.api.v2.fagsystem.EventMetadata
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.intellij.lang.annotations.Language
import org.springframework.kafka.listener.MessageListener
import org.springframework.stereotype.Service

@Service
class FagsystemKafkaListener(
    private val fagsystemIntegrasjonService: FagsystemIntegrasjonService,
) : MessageListener<String, String> {
    val log = TracedLogger.getLogger<FagsystemKafkaListener>()
    private val ytelseForTopics = Ytelse.ytelser().associateBy { it.kafkaTopic }

    override fun onMessage(data: ConsumerRecord<String, String>) {
        val ytelse = ytelseForTopics[data.topic()]
        val logContext = SecureLog.Context.tom()
        if (ytelse == null) {
            log.medContext(logContext) {
                error("Fant ikke ytelse for topic {}. Innhold: {}", data.topic(), data.value())
            }
            return
        }
        SecureLog.medContext(logContext) {
            info("Mottok melding fra fagsystem via kafka topic {}, melding: {}", data.topic(), data.value())
        }
        try {
            log.medContext(logContext) {
                info("Mottok melding fra kafka via kafka topic {}", data.topic())
            }
            håndterMelding(ytelse, data.value())
        } catch (e: Exception) {
            log.medContext(logContext) {
                error("Kunne ikke håndtere svar fra fagsystem", e)
            }
            throw e
        }
    }

    fun håndterMelding(
        ytelse: Ytelse,
        @Language("JSON") melding: String,
    ) {
        val obj = objectMapper.readTree(melding)
        val header = objectMapper.convertValue<EventMetadata<*>>(obj)
        when (header) {
            FagsysteminfoSvarHendelse.METADATA -> {
                val fagsysteminfo = objectMapper.treeToValue<FagsysteminfoSvarHendelse>(obj)
                fagsystemIntegrasjonService.håndter(ytelse, fagsysteminfo)
            }
        }
        SecureLog.medContext(SecureLog.Context.tom()) {
            info(
                "Ferdig med håndtering av melding med type {}. Mottatt for ytelse {}",
                header.hendelsestype,
                ytelse.tilFagsystemDTO(),
            )
        }
    }
}
