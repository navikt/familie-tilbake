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
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettBehandlingAutomatisk(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        return opprettFørstegangsbehandling(opprettTilbakekrevingRequest)
    }

    fun opprettBehandlingManuell(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) : Behandling{
        TODO("Ikke implementert ennå")
    }

    private fun opprettFørstegangsbehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        val ytelsestype = Ytelsestype.valueOf(opprettTilbakekrevingRequest.ytelsestype.name)
        val fagsystem = Fagsystem.fraYtelsestype(ytelsestype)
        val eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId
        val eksternId = opprettTilbakekrevingRequest.eksternId
        logger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                    "og eksternId=$eksternId")
        secureLogger.info("Oppretter Tilbakekrevingsbehandling for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                          " og personIdent=${opprettTilbakekrevingRequest.personIdent.ident}")

        kanBehandlingOpprettes(ytelsestype, eksternFagsakId, eksternId)
        // oppretter fagsak
        val fagsak = opprettFagsak(opprettTilbakekrevingRequest, ytelsestype, fagsystem)
        fagsakRepository.insert(fagsak)
        val behandling = BehandlingMapper.tilDomeneBehandling(opprettTilbakekrevingRequest, fagsystem, fagsak)
        behandlingRepository.insert(behandling)
        //TODO historikk

        return behandling
    }

    private fun kanBehandlingOpprettes(ytelsestype: Ytelsestype,
                                       eksternFagsakId: String,
                                       eksternId: String) {
        val behandling: Behandling? = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype.kode, eksternFagsakId)
        if (behandling != null) {
            val feilMelding = "Det finnes allerede en åpen behandling for ytelsestype=$ytelsestype " +
                              "og eksternFagsakId=$eksternFagsakId, kan ikke opprettes en ny.";
            throw Feil(message = feilMelding, frontendFeilmelding = feilMelding)
        }

        //hvis behandlingen er henlagt,kan opprettes ny behandling
        val avsluttetBehandlinger = behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(eksternId)
        if (avsluttetBehandlinger.isNotEmpty()) {
            val sisteAvsluttetBehandling: Behandling = avsluttetBehandlinger.first()
            val erSisteBehandlingHenlagt: Boolean =
                    sisteAvsluttetBehandling.resultater.any { Behandlingsresultat().erBehandlingHenlagt() }
            if (!erSisteBehandlingHenlagt) {
                val feilMelding = "Det finnes allerede en avsluttet behandling for ytelsestype=$ytelsestype " +
                                  "og eksternFagsakId=$eksternFagsakId som ikke er henlagt, kan ikke opprettes en ny."
                throw Feil(message = feilMelding, frontendFeilmelding = feilMelding)
            }
        }

    }

    private fun opprettFagsak(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                              ytelsestype: Ytelsestype,
                              fagsystem: Fagsystem): Fagsak {
        val bruker = Bruker(opprettTilbakekrevingRequest.personIdent.ident,
                            Bruker.velgSpråkkode(opprettTilbakekrevingRequest.språkkode))
        return Fagsak(bruker = bruker,
                      eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                      ytelsestype = ytelsestype.kode,
                      fagsystem = fagsystem)
    }


}
