package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype.TILBAKEKREVING
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val personService: PersonService) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettBehandlingAutomatisk(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        return opprettFørstegangsbehandling(opprettTilbakekrevingRequest)
    }

    fun opprettBehandlingManuell(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Behandling {
        TODO("Ikke implementert ennå")
    }

    @Transactional(readOnly = true)
    fun hentBehandling(behandlingId: UUID): BehandlingDto {
        val data = behandlingRepository.findById(behandlingId)
        if (data.isPresent) {
            val behandling = data.get()
            return BehandlingMapper.tilRespons(behandling, kanHenleggeBehandling(behandling))
        }
        throw Feil(message = "Behandling finnes ikke for behandlingId=$behandlingId",
                   frontendFeilmelding = "Behandling finnes ikke for behandlingId=$behandlingId")
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

        return behandling
    }

    private fun kanBehandlingOpprettes(ytelsestype: Ytelsestype,
                                       eksternFagsakId: String,
                                       eksternId: String) {
        val behandling: Behandling? = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        if (behandling != null) {
            val feilMelding = "Det finnes allerede en åpen behandling for ytelsestype=$ytelsestype " +
                              "og eksternFagsakId=$eksternFagsakId, kan ikke opprette en ny."
            throw Feil(message = feilMelding, frontendFeilmelding = feilMelding)
        }

        //hvis behandlingen er henlagt, kan det opprettes ny behandling
        val avsluttetBehandlinger = behandlingRepository.finnAvsluttetTilbakekrevingsbehandlinger(eksternId)
        if (avsluttetBehandlinger.isNotEmpty()) {
            val sisteAvsluttetBehandling: Behandling = avsluttetBehandlinger.first()
            val erSisteBehandlingHenlagt: Boolean =
                    sisteAvsluttetBehandling.resultater.any { Behandlingsresultat().erBehandlingHenlagt() }
            if (!erSisteBehandlingHenlagt) {
                val feilMelding = "Det finnes allerede en avsluttet behandling for ytelsestype=$ytelsestype " +
                                  "og eksternFagsakId=$eksternFagsakId som ikke er henlagt, kan ikke opprette en ny."
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
                      ytelsestype = ytelsestype,
                      fagsystem = fagsystem)
    }

    private fun kanHenleggeBehandling(behandling: Behandling): Boolean {
        var kanHenlegges = true
        if (TILBAKEKREVING == behandling.type) {
            kanHenlegges = !behandling.erAvsluttet() && (!behandling.manueltOpprettet &&
                                                         behandling.opprettetTidspunkt
                                                                 .isBefore(LocalDate.now()
                                                                                   .atStartOfDay()
                                                                                   .minusDays(OPPRETTELSE_DAGER_BEGRENSNING)))
        }
        return kanHenlegges
    }

    companion object {

        const val OPPRETTELSE_DAGER_BEGRENSNING = 6L
    }

}
