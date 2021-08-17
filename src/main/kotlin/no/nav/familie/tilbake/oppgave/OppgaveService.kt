package no.nav.familie.tilbake.oppgave

import io.micrometer.core.instrument.Metrics
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OppgaveService(private val behandlingRepository: BehandlingRepository,
                     private val fagsakRepository: FagsakRepository,
                     private val integrasjonerClient: IntegrasjonerClient,
                     private val personService: PersonService) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val antallOppgaveTyper = Oppgavetype.values().associateWith {
        Metrics.counter("oppgave.opprettet", "type", it.name)
    }

    fun finnOppgaveForBehandling(behandlingId: UUID, oppgavetype: Oppgavetype): Oppgave {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakId = behandling.fagsakId
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

        val finnOppgaveResponse =
                integrasjonerClient.finnOppgaver(FinnOppgaveRequest(behandlingstype = Behandlingstype.Tilbakekreving,
                                                                    oppgavetype = oppgavetype,
                                                                    saksreferanse = behandling.eksternBrukId.toString(),
                                                                    tema = fagsak.ytelsestype.tilTema()))
        if (finnOppgaveResponse.oppgaver.size > 1) {
            log.error("er mer enn en åpen oppgave for behandlingen")
        }
        return finnOppgaveResponse.oppgaver.first()
    }

    fun opprettOppgave(behandlingId: UUID,
                       oppgavetype: Oppgavetype,
                       beskrivelse: String?,
                       fristForFerdigstillelse: LocalDate): OppgaveResponse {

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakId = behandling.fagsakId
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val aktorId = personService.hentAktivAktørId(fagsak.bruker.ident, fagsak.fagsystem)


        val opprettOppgave = OpprettOppgaveRequest(ident = OppgaveIdentV2(ident = aktorId,
                                                                          gruppe = IdentGruppe.AKTOERID),
                                                   saksId = behandling.eksternBrukId.toString(),
                                                   tema = fagsak.ytelsestype.tilTema(),
                                                   oppgavetype = oppgavetype,
                                                   behandlesAvApplikasjon = "familie-tilbake",
                                                   fristFerdigstillelse = fristForFerdigstillelse,
                                                   beskrivelse = lagOppgaveTekst(fagsak.eksternFagsakId,
                                                                                 behandling.eksternBrukId.toString(),
                                                                                 fagsak.fagsystem.name,
                                                                                 beskrivelse),
                                                   enhetsnummer = behandling.behandlendeEnhet,
                                                   behandlingstype = Behandlingstype.Tilbakekreving.value,
                                                   behandlingstema = null)

        val opprettetOppgaveId = integrasjonerClient.opprettOppgave(opprettOppgave)

        antallOppgaveTyper[oppgavetype]!!.increment()

        return opprettetOppgaveId
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        return integrasjonerClient.patchOppgave(patchOppgave)
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String): OppgaveResponse {
        return integrasjonerClient.fordelOppgave(oppgaveId, saksbehandler)
    }

    fun ferdigstillOppgave(behandlingId: UUID, oppgavetype: Oppgavetype) {

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val finnOppgaveRequest = FinnOppgaveRequest(behandlingstype = Behandlingstype.Tilbakekreving,
                                                    oppgavetype = oppgavetype,
                                                    saksreferanse = behandling.eksternBrukId.toString(),
                                                    tema = fagsak.ytelsestype.tilTema())

        val finnOppgaveResponse =
                integrasjonerClient.finnOppgaver(finnOppgaveRequest)
        if (finnOppgaveResponse.oppgaver.size > 1) {
            LOG.error("er mer enn en åpen oppgave for behandlingen")
            SECURELOG.error("Mer enn en oppgave åpen for behandling ${behandling.eksternBrukId}, " +
                            "$finnOppgaveRequest, $finnOppgaveResponse")
        } else if (finnOppgaveResponse.oppgaver.isEmpty()) {
            LOG.error("Fant ingen oppgave å ferdigstille")
            SECURELOG.error("Fant ingen oppgave å ferdigstille ${behandling.eksternBrukId}, " +
                            "$finnOppgaveRequest, $finnOppgaveResponse")
        }
        integrasjonerClient.ferdigstillOppgave(finnOppgaveResponse.oppgaver[0].id!!)

    }


    private fun lagOppgaveTekst(eksternFagsakId: String,
                                eksternbrukBehandlingID: String,
                                fagsystem: String,
                                beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } + "--- Opprettet av familie-tilbake ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "https://familie-tilbake-frontend.dev.intern.nav.no/fagsystem/${fagsystem}/fagsak/${eksternFagsakId}/behandling/" +
               eksternbrukBehandlingID
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)
        val SECURELOG: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
