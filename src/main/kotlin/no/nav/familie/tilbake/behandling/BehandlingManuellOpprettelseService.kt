package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingManuellOpprettelseService(private val behandlingService: BehandlingService) {

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