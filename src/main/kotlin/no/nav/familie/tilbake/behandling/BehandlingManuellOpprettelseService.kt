package no.nav.familie.tilbake.behandling

import no.nav.tilbakekreving.kontrakter.tilbakekreving.Behandlingstype
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Ytelsestype
import no.nav.tilbakekreving.kontrakter.tilbakekreving.v1.HentFagsystemsbehandling
import no.nav.tilbakekreving.kontrakter.tilbakekreving.v1.OpprettTilbakekrevingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingManuellOpprettelseService(
    private val behandlingService: BehandlingService,
) {
    @Transactional
    fun opprettBehandlingManuell(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
        ansvarligSaksbehandler: String,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
    ) {
        val opprettTilbakekrevingRequest =
            lagOpprettBehandlingsrequest(
                eksternFagsakId = eksternFagsakId,
                ytelsestype = ytelsestype,
                eksternId = eksternId,
                fagsystemsbehandlingData = fagsystemsbehandlingData,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
            )
        behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
    }

    private fun lagOpprettBehandlingsrequest(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
        fagsystemsbehandlingData: HentFagsystemsbehandling,
        ansvarligSaksbehandler: String,
    ): OpprettTilbakekrevingRequest =
        OpprettTilbakekrevingRequest(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
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
            varsel = null,
            institusjon = fagsystemsbehandlingData.institusjon,
            begrunnelseForTilbakekreving = null,
        )
}
