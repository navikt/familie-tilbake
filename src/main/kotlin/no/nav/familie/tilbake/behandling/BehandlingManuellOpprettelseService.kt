package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingManuellOpprettelseService(private val requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository,
                                          private val kafkaProducer: KafkaProducer,
                                          private val behandlingService: BehandlingService) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendHentFagsystemsbehandlingRequest(eksternFagsakId: String,
                                            ytelsestype: Ytelsestype,
                                            eksternId: String) {
        val eksisterendeRequestSendt =
                requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId, ytelsestype, eksternId)
        if (eksisterendeRequestSendt == null) {
            val requestSendt =
                    requestSendtRepository.insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = eksternFagsakId,
                                                                                       ytelsestype = ytelsestype,
                                                                                       eksternId = eksternId))
            val request = HentFagsystemsbehandlingRequest(eksternFagsakId, ytelsestype, eksternId)
            kafkaProducer.sendHentFagsystemsbehandlingRequest(requestSendt.id, request)
        }
    }

    @Transactional
    fun lagreHentFagsystemsbehandlingRespons(requestId: UUID,
                                             respons: String) {
        val fagsystemsbehandlingRequestSendt = requestSendtRepository.findByIdOrThrow(requestId)
        requestSendtRepository.update(fagsystemsbehandlingRequestSendt.copy(respons = respons))
    }

    @Transactional
    fun hentFagsystemsbehandlingRequestSendt(eksternFagsakId: String,
                                             ytelsestype: Ytelsestype,
                                             eksternId: String): HentFagsystemsbehandlingRequestSendt? {
        return requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                                      ytelsestype,
                                                                                      eksternId)
    }

    @Transactional
    fun opprettBehandlingManuell(eksternFagsakId: String,
                                 ytelsestype: Ytelsestype,
                                 eksternId: String,
                                 ansvarligSaksbehandler: String,
                                 hentFagsystemsbehandlingRespons: String) {
        val fagsystemsbehandlingData = objectMapper.readValue(hentFagsystemsbehandlingRespons,
                                                              HentFagsystemsbehandlingRespons::class.java)
        val opprettTilbakekrevingRequest = lagOpprettBehandlingsrequest(eksternFagsakId = eksternFagsakId,
                                                                        ytelsestype = ytelsestype,
                                                                        eksternId = eksternId,
                                                                        fagsystemsbehandlingData = fagsystemsbehandlingData,
                                                                        ansvarligSaksbehandler = ansvarligSaksbehandler)
        behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
    }

    private fun lagOpprettBehandlingsrequest(eksternFagsakId: String,
                                             ytelsestype: Ytelsestype,
                                             eksternId: String,
                                             fagsystemsbehandlingData: HentFagsystemsbehandlingRespons,
                                             ansvarligSaksbehandler: String): OpprettTilbakekrevingRequest {
        return OpprettTilbakekrevingRequest(fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                                            ytelsestype = ytelsestype,
                                            eksternFagsakId = eksternFagsakId,
                                            eksternId = eksternId,
                                            behandlingstype = Behandlingstype.TILBAKEKREVING,
                                            manueltOpprettet = true,
                                            saksbehandlerIdent = ansvarligSaksbehandler,
                                            personIdent = fagsystemsbehandlingData.personIdent,
                                            språkkode = fagsystemsbehandlingData.språkkode,
                                            enhetId = fagsystemsbehandlingData.enhetId,
                                            enhetsnavn = fagsystemsbehandlingData.enhetsnavn,
                                            revurderingsvedtaksdato = fagsystemsbehandlingData.revurderingsvedtaksdato,
                                            faktainfo = fagsystemsbehandlingData.faktainfo,
                                            verge = fagsystemsbehandlingData.verge,
                                            varsel = null)
    }
}