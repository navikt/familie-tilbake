package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kontrakter.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.tilbake.kontrakter.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HentFagsystemsbehandlingService(
    private val requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository,
    private val kafkaProducer: KafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(HentFagsystemsbehandlingService::class.java)

    @Transactional
    fun sendHentFagsystemsbehandlingRequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
    ) {
        val eksisterendeRequestSendt =
            requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId, ytelsestype, eksternId)
        if (eksisterendeRequestSendt == null) {
            opprettOgSendHentFagsystembehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
        }
    }

    private fun opprettOgSendHentFagsystembehandlingRequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
    ) {
        val requestSendt =
            requestSendtRepository.insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = eksternFagsakId,
                    ytelsestype = ytelsestype,
                    eksternId = eksternId,
                ),
            )

        val request = HentFagsystemsbehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
        kafkaProducer.sendHentFagsystemsbehandlingRequest(requestSendt.id, request, SecureLog.Context.medBehandling(eksternFagsakId, requestSendt.id.toString()))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun slettOgSendNyHentFagsystembehandlingRequest(
        requestSendtId: UUID,
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
    ) {
        fjernHentFagsystemsbehandlingRequest(requestSendtId)
        opprettOgSendHentFagsystembehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
    }

    @Transactional
    fun lagreHentFagsystemsbehandlingRespons(
        requestId: UUID,
        response: String,
    ) {
        val fagsystemsbehandlingRequestSendt = requestSendtRepository.findByIdOrThrow(requestId)
        logger.info("Fagsystemsbehandlingsdata er mottatt i kafka med key={}", requestId)
        SecureLog
            .utenBehandling(fagsystemsbehandlingRequestSendt.eksternFagsakId) {
                info("Fagsystemsbehandlingsdata er mottatt i kafka {}", response)
            }
        requestSendtRepository.update(fagsystemsbehandlingRequestSendt.copy(respons = response))
    }

    @Transactional
    fun hentFagsystemsbehandlingRequestSendt(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
    ): HentFagsystemsbehandlingRequestSendt? =
        requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(
            eksternFagsakId,
            ytelsestype,
            eksternId,
        )

    @Transactional
    fun fjernHentFagsystemsbehandlingRequest(requestId: UUID) {
        requestSendtRepository.deleteById(requestId)
    }

    fun lesRespons(respons: String): HentFagsystemsbehandlingRespons = objectMapper.readValue(respons, HentFagsystemsbehandlingRespons::class.java)
}
