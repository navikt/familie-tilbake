package no.nav.familie.tilbake.integration.kafka

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.KafkaConfig
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kontrakter.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

interface KafkaProducer {
    fun sendSaksdata(
        behandlingId: UUID,
        request: Behandlingstilstand,
        logContext: SecureLog.Context,
    )

    fun sendVedtaksdata(
        behandlingId: UUID,
        request: Vedtaksoppsummering,
        logContext: SecureLog.Context,
    )

    fun sendRåFagsystemsbehandlingResponse(
        behandlingId: UUID?,
        response: String,
    )

    fun sendHentFagsystemsbehandlingRequest(
        requestId: UUID,
        request: HentFagsystemsbehandlingRequest,
        logContext: SecureLog.Context,
    )
}

@Service
@Profile("!integrasjonstest & !e2e & !local")
class DefaultKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: KafkaProperties,
) : KafkaProducer {
    private val log = TracedLogger.getLogger<DefaultKafkaProducer>()

    override fun sendSaksdata(
        behandlingId: UUID,
        request: Behandlingstilstand,
        logContext: SecureLog.Context,
    ) {
        sendKafkamelding(
            behandlingId = behandlingId,
            topic = KafkaConfig.SAK_TOPIC,
            key = request.behandlingUuid.toString(),
            request = request,
            logContext = logContext,
        )
    }

    override fun sendVedtaksdata(
        behandlingId: UUID,
        request: Vedtaksoppsummering,
        logContext: SecureLog.Context,
    ) {
        sendKafkamelding(
            behandlingId = behandlingId,
            topic = KafkaConfig.VEDTAK_TOPIC,
            key = request.behandlingUuid.toString(),
            request = request,
            logContext = logContext,
        )
    }

    override fun sendHentFagsystemsbehandlingRequest(
        requestId: UUID,
        request: HentFagsystemsbehandlingRequest,
        logContext: SecureLog.Context,
    ) {
        sendKafkamelding(
            behandlingId = requestId,
            topic = kafkaProperties.hentFagsystem.requestTopic,
            key = requestId.toString(),
            request = request,
            logContext = logContext,
        )
    }

    override fun sendRåFagsystemsbehandlingResponse(
        behandlingId: UUID?,
        response: String,
    ) {
        val producerRecord =
            ProducerRecord(
                kafkaProperties.hentFagsystem.responseTopic,
                behandlingId?.toString(),
                response,
            )
        kafkaTemplate.send(producerRecord)
    }

    private fun sendKafkamelding(
        behandlingId: UUID,
        topic: String,
        key: String,
        request: Any,
        logContext: SecureLog.Context,
    ) {
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(topic, key, melding)
        kotlin
            .runCatching {
                val callback = kafkaTemplate.send(producerRecord).get()
                log.medContext(logContext) {
                    info(
                        "Melding på topic $topic for $behandlingId med $key er sendt. " +
                            "Fikk offset ${callback?.recordMetadata?.offset()}",
                    )
                }
            }.onFailure {
                val feilmelding =
                    "Melding på topic $topic kan ikke sendes for $behandlingId med $key. " +
                        "Feiler med ${it.message}"
                log.medContext(logContext) {
                    warn(feilmelding)
                }
                throw Feil(
                    message = feilmelding,
                    logContext = logContext,
                )
            }
    }
}

@Service
@Profile("e2e", "integrasjonstest", "local")
class E2EKafkaProducer : KafkaProducer {
    override fun sendSaksdata(
        behandlingId: UUID,
        request: Behandlingstilstand,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Skipper sending av saksstatistikk for behandling $behandlingId fordi kafka ikke er enablet")
        }
    }

    override fun sendVedtaksdata(
        behandlingId: UUID,
        request: Vedtaksoppsummering,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Skipper sending av vedtaksstatistikk for behandling $behandlingId fordi kafka ikke er enablet")
        }
    }

    override fun sendRåFagsystemsbehandlingResponse(
        behandlingId: UUID?,
        request: String,
    ) {
        log.medContext(SecureLog.Context.tom()) {
            info("Skipper sending av rå vedtaksdata for behandling $behandlingId fordi kafka ikke er enablet")
        }
    }

    override fun sendHentFagsystemsbehandlingRequest(
        requestId: UUID,
        request: HentFagsystemsbehandlingRequest,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Skipper sending av info-request for fagsystembehandling ${request.eksternId} fordi kafka ikke er enablet")
        }
    }

    companion object {
        private val log = TracedLogger.getLogger<E2EKafkaProducer>()
    }
}
