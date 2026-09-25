package no.nav.tilbakekreving.integrasjoner

import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.fagsystem.EventMetadata
import no.nav.tilbakekreving.api.v2.fagsystem.Kafkamelding
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.events.HendelseEventDto
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandlingRequest
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

@Service
@Primary
class KafkaProducerStub() : KafkaProducer {
    private val saksdata = ConcurrentHashMap<UUID, CopyOnWriteArrayList<Behandlingstilstand>>()
    private val vedtak = ConcurrentHashMap<UUID, CopyOnWriteArrayList<Vedtaksoppsummering>>()
    private val kafkameldinger = ConcurrentHashMap<String, CopyOnWriteArrayList<Pair<EventMetadata<*>, Kafkamelding>>>()
    private val hendelser = ConcurrentHashMap<String, CopyOnWriteArrayList<HendelseEventDto>>()

    private val hendelseHandlerFor = ConcurrentHashMap<HendelseHandlerKey, () -> Unit>()

    override fun <K : Kafkamelding> sendKafkaEvent(
        kafkamelding: K,
        metadata: EventMetadata<K>,
        vedtakGjelderId: String,
        ytelse: Ytelse,
        logContext: SecureLog.Context,
    ) {
        kafkameldinger.computeIfAbsent(kafkamelding.eksternFagsakId) { CopyOnWriteArrayList() }.add(metadata to kafkamelding)
    }

    override fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand, logContext: SecureLog.Context) {
        saksdata.computeIfAbsent(behandlingId) { CopyOnWriteArrayList() }.add(request)
    }

    override fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering, logContext: SecureLog.Context) {
        vedtak.computeIfAbsent(behandlingId) { CopyOnWriteArrayList() }.add(request)
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
        hendelser.computeIfAbsent(hendelse.eksternFagsakId) { CopyOnWriteArrayList() }.add(hendelse)
    }

    fun finnSaksdata(behandlingId: UUID): List<Behandlingstilstand> = saksdata[behandlingId] ?: emptyList()

    fun finnVedtaksoppsummering(behandlingId: UUID): List<Vedtaksoppsummering> = vedtak[behandlingId] ?: emptyList()

    fun finnKafkamelding(eksternFagsakId: String): List<Pair<EventMetadata<*>, Kafkamelding>> = kafkameldinger[eksternFagsakId] ?: emptyList()

    fun finnHendelser(eksternFagsakId: String): List<HendelseEventDto> = hendelser[eksternFagsakId] ?: emptyList()

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

    data class HendelseHandlerKey(
        val type: KClass<out HendelseEventDto>,
        val eksternFagsakId: String,
    )
}
