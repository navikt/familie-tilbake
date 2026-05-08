package no.nav.tilbakekreving.integrasjoner

import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.fagsystem.EventMetadata
import no.nav.tilbakekreving.api.v2.fagsystem.Kafkamelding
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.events.HendelseEventDto
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandlingRequest
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
@Primary
class KafkaProducerStub() : KafkaProducer {
    private val saksdata = mutableMapOf<UUID, MutableList<Behandlingstilstand>>()
    private val vedtak = mutableMapOf<UUID, MutableList<Vedtaksoppsummering>>()
    private val kafkameldinger = mutableMapOf<String, MutableList<Pair<EventMetadata<*>, Kafkamelding>>>()
    private val hendelser = mutableMapOf<String, MutableList<HendelseEventDto>>()

    private val fagsystemInfoSvarHandlers = mutableMapOf<String, () -> Unit>()
    private val handlerFor = mutableMapOf<HandkerKey, () -> Unit>()
    private val hendelseHandlerFor = mutableMapOf<HendelseHandlerKey, () -> Unit>()

    override fun <K : Kafkamelding> sendKafkaEvent(
        kafkamelding: K,
        metadata: EventMetadata<K>,
        vedtakGjelderId: String,
        ytelse: Ytelse,
        logContext: SecureLog.Context,
    ) {
        handlerFor.remove(HandkerKey(metadata, kafkamelding.eksternFagsakId))?.invoke()
        kafkameldinger.computeIfAbsent(kafkamelding.eksternFagsakId) { mutableListOf() }.add(metadata to kafkamelding)
        when (metadata) {
            FagsysteminfoBehovHendelse.METADATA -> {
                fagsystemInfoSvarHandlers.remove(kafkamelding.eksternFagsakId)?.invoke()
            }
        }
    }

    override fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand, logContext: SecureLog.Context) {
        saksdata.computeIfAbsent(behandlingId) { mutableListOf() }.add(request)
    }

    override fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering, logContext: SecureLog.Context) {
        vedtak.computeIfAbsent(behandlingId) { mutableListOf() }.add(request)
    }

    override fun sendRåFagsystemsbehandlingResponse(behandlingId: UUID, response: String) {}

    override fun sendHentFagsystemsbehandlingRequest(requestId: UUID, request: HentFagsystemsbehandlingRequest, logContext: SecureLog.Context) {}

    override fun sendHendelseEvent(
        hendelse: HendelseEventDto,
        vedtakGjelderId: String,
        ytelse: Ytelse,
        logContext: SecureLog.Context,
    ) {
        hendelseHandlerFor[HendelseHandlerKey(hendelse::class, hendelse.eksternFagsakId)]?.let { handler -> handler() }
        hendelser.computeIfAbsent(hendelse.eksternFagsakId) { mutableListOf() }.add(hendelse)
    }

    fun finnSaksdata(behandlingId: UUID): List<Behandlingstilstand> = saksdata[behandlingId] ?: emptyList()

    fun finnVedtaksoppsummering(behandlingId: UUID): List<Vedtaksoppsummering> = vedtak[behandlingId] ?: emptyList()

    fun finnKafkamelding(eksternFagsakId: String): List<Pair<EventMetadata<*>, Kafkamelding>> = kafkameldinger[eksternFagsakId] ?: emptyList()

    fun finnHendelser(eksternFagsakId: String): List<HendelseEventDto> = hendelser[eksternFagsakId] ?: emptyList()

    fun settFagsysteminfoSvar(eksternFagsakId: String, handler: () -> Unit) {
        fagsystemInfoSvarHandlers[eksternFagsakId] = handler
    }

    fun vedMelding(metadata: EventMetadata<*>, fagsystemId: String, callback: () -> Unit) {
        handlerFor[HandkerKey(metadata, fagsystemId)] = callback
    }

    fun vedHendelse(key: HendelseHandlerKey, callback: () -> Unit) {
        hendelseHandlerFor[key] = callback
    }

    companion object {
        inline fun <reified T : Kafkamelding> KafkaProducerStub.finnKafkamelding(eksternFagsakId: String, type: EventMetadata<T>): List<T> {
            return finnKafkamelding(eksternFagsakId)
                .filter { (metadata, _) -> metadata == type }
                .map { (_, value) -> value }
                .map { it.shouldBeInstanceOf() }
        }

        inline fun <reified T : HendelseEventDto> KafkaProducerStub.finnHendelse(eksternFagsakId: String): List<T> {
            return finnHendelser(eksternFagsakId).filterIsInstance<T>()
        }

        inline fun <reified T : HendelseEventDto> KafkaProducerStub.vedHendelse(fagsystemId: String, noinline callback: () -> Unit) {
            vedHendelse(HendelseHandlerKey(T::class, fagsystemId), callback)
        }
    }

    data class HandkerKey(
        val metadata: EventMetadata<*>,
        val eksternFagsakId: String,
    )

    data class HendelseHandlerKey(
        val type: KClass<out HendelseEventDto>,
        val eksternFagsakId: String,
    )
}
