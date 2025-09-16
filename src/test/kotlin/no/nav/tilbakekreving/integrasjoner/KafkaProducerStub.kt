package no.nav.tilbakekreving.integrasjoner

import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.Kafkamelding
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandlingRequest
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Primary
class KafkaProducerStub : KafkaProducer {
    private val saksdata = mutableMapOf<UUID, MutableList<Behandlingstilstand>>()
    private val vedtak = mutableMapOf<UUID, MutableList<Vedtaksoppsummering>>()
    private val kafkameldinger = mutableMapOf<String, MutableList<Kafkamelding>>()

    override fun sendKafkaEvent(
        kafkamelding: Kafkamelding,
        vedtakGjelderId: String,
        ytelse: Ytelse,
        logContext: SecureLog.Context,
    ) {
        kafkameldinger.computeIfAbsent(kafkamelding.eksternFagsakId) { mutableListOf() }.add(kafkamelding)
    }

    override fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand, logContext: SecureLog.Context) {
        saksdata.computeIfAbsent(behandlingId) { mutableListOf() }.add(request)
    }

    override fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering, logContext: SecureLog.Context) {
        vedtak.computeIfAbsent(behandlingId) { mutableListOf() }.add(request)
    }

    override fun sendRåFagsystemsbehandlingResponse(behandlingId: UUID?, response: String) {}

    override fun sendHentFagsystemsbehandlingRequest(requestId: UUID, request: HentFagsystemsbehandlingRequest, logContext: SecureLog.Context) {}

    fun finnSaksdata(behandlingId: UUID): List<Behandlingstilstand> = saksdata[behandlingId] ?: emptyList()

    fun finnVedtaksoppsummering(behandlingId: UUID): List<Vedtaksoppsummering> = vedtak[behandlingId] ?: emptyList()

    fun finnKafkamelding(eksternFagsakId: String): List<Kafkamelding> = kafkameldinger[eksternFagsakId] ?: emptyList()
}
