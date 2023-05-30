package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HentFagsystemsbehandlingService(
    private val requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository,
    private val kafkaProducer: KafkaProducer
) {


    @Transactional
    fun sendHentFagsystemsbehandlingRequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String
    ) {
        val eksisterendeRequestSendt =
            requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId, ytelsestype, eksternId)
        if (eksisterendeRequestSendt == null) {
            val requestSendt =
                requestSendtRepository.insert(
                    HentFagsystemsbehandlingRequestSendt(
                        eksternFagsakId = eksternFagsakId,
                        ytelsestype = ytelsestype,
                        eksternId = eksternId
                    )
                )
            val request = HentFagsystemsbehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
            kafkaProducer.sendHentFagsystemsbehandlingRequest(requestSendt.id, request)
        }
    }

    @Transactional
    fun lagreHentFagsystemsbehandlingRespons(
        requestId: UUID,
        respons: String
    ) {
        val fagsystemsbehandlingRequestSendt = requestSendtRepository.findByIdOrThrow(requestId)
        requestSendtRepository.update(fagsystemsbehandlingRequestSendt.copy(respons = respons))
    }

    @Transactional
    fun hentFagsystemsbehandlingRequestSendt(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String
    ): HentFagsystemsbehandlingRequestSendt? {
        return requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(
            eksternFagsakId,
            ytelsestype,
            eksternId
        )
    }

    @Transactional
    fun fjernHentFagsystemsbehandlingRequest(requestId: UUID) {
        requestSendtRepository.deleteById(requestId)
    }

    fun lesRespons(respons: String): HentFagsystemsbehandlingRespons {
        return objectMapper.readValue(respons, HentFagsystemsbehandlingRespons::class.java)
    }
}
