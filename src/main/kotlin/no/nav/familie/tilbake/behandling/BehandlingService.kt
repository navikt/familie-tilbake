package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun opprettBehandlingAutomatisk(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        return opprettFørstegangsbehandling(opprettTilbakekrevingRequest)
    }

    fun opprettBehandlingManuell(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        TODO("Ikke implementert ennå")
    }

    private fun opprettFørstegangsbehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val fagsystem = opprettTilbakekrevingRequest.fagsystem
        val eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId
        val eksternId = opprettTilbakekrevingRequest.eksternId
        logger.info("Oppretter Tilbakekrevingsbehandling for fagsystem=$fagsystem,eksternFagsakId=$eksternFagsakId " +
                    "og eksternId=$eksternId")
        secureLogger.info("Oppretter Tilbakekrevingsbehandling for fagsystem=$fagsystem,eksternFagsakId=$eksternFagsakId " +
                          " og personIdent=${opprettTilbakekrevingRequest.personIdent.ident}")
        kanBehandlingOpprettes(fagsystem, eksternFagsakId, eksternId)

        // oppretter fagsak
        val fagsak = opprettFagsak(opprettTilbakekrevingRequest)
        fagsakRepository.insert(fagsak)
        val behandling = BehandlingMapper.tilDomeneBehandling(opprettTilbakekrevingRequest, fagsystem, fagsak)
        behandlingRepository.insert(behandling)
        //TODO historikk

        return behandling
    }

    private fun kanBehandlingOpprettes(fagsystem: String,
                                       eksternFagsakId: String,
                                       eksternId: String) {
        val behandling: Behandling? = behandlingRepository.finnÅpenTilbakekrevingsbehandling(fagsystem, eksternFagsakId)
        if (behandling != null) {
            throw Feil("Det finnes allerede en åpen behandling for fagsystem=$fagsystem " +
                       "og eksternFagsakId=$eksternFagsakId, kan ikke opprettes en ny.")
        }

        //hvis behandlingen er henlagt,kan opprettes ny behandling
        val avsluttetBehandlinger = behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(eksternId)
        if (avsluttetBehandlinger.isNotEmpty()) {
            val sisteAvsluttetBehandling: Behandling = avsluttetBehandlinger.get(0)
            val erSisteBehandlingHenlagt: Boolean = sisteAvsluttetBehandling.resultater.stream()
                    .anyMatch { Behandlingsresultat().erBehandlingHenlagt() }
            if (!erSisteBehandlingHenlagt) {
                throw Feil("Det finnes allerede en avsluttet behandling for fagsystem=$fagsystem " +
                           "og eksternFagsakId=$eksternFagsakId som ikke er henlagt, kan ikke opprettes en ny.")
            }
        }

    }

    private fun opprettFagsak(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Fagsak {
        val bruker = Bruker(opprettTilbakekrevingRequest.personIdent.ident, opprettTilbakekrevingRequest.språkkode!!)
        val ytelsestype = Ytelsestype.valueOf(opprettTilbakekrevingRequest.fagsystem)
        return Fagsak(bruker = bruker,
                      eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                      ytelsestype = ytelsestype,
                      fagsystem = Fagsystem.fraYtelsestype(ytelsestype))
    }


}
