package no.nav.familie.tilbake.oppgave

import io.micrometer.core.instrument.Metrics
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ManglerOppgaveFeil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.oppgave.Behandlingstype
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.familie.tilbake.kontrakter.oppgave.IdentGruppe
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgaveIdentV2
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kontrakter.oppgave.OppgaveResponse
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.familie.tilbake.kontrakter.oppgave.StatusEnum
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.person.PersonService
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OppgaveService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val integrasjonerClient: IntegrasjonerClient,
    private val personService: PersonService,
    private val taskService: TaskService,
    @Value("\${tilbakekreving.frontendUrl}")
    private val frontendUrl: String,
) {
    private val log = TracedLogger.getLogger<OppgaveService>()

    private val antallOppgaveTyper =
        Oppgavetype.values().associateWith {
            Metrics.counter("oppgave.opprettet", "type", it.name)
        }

    fun finnOppgaveForBehandlingUtenOppgaveType(behandlingId: UUID): Oppgave {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

        val finnOppgaveRequest =
            FinnOppgaveRequest(
                saksreferanse = behandling.eksternBrukId.toString(),
                tema = fagsak.ytelsestype.tilTema(),
            )
        val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
        when {
            finnOppgaveResponse.oppgaver.size > 1 -> {
                SecureLog.medContext(logContext) {
                    error(
                        "Mer enn en oppgave åpen for behandling {}, {}, {}",
                        behandling.eksternBrukId,
                        finnOppgaveRequest,
                        finnOppgaveResponse,
                    )
                }
                throw Feil(
                    message = "Har mer enn en åpen oppgave for behandling ${behandling.eksternBrukId}",
                    logContext = logContext,
                )
            }

            finnOppgaveResponse.oppgaver.isEmpty() -> {
                SecureLog.medContext(logContext) {
                    error(
                        "Fant ingen oppgave for behandling {} på fagsak {}, {}, {}",
                        behandling.eksternBrukId,
                        fagsak.eksternFagsakId,
                        finnOppgaveRequest,
                        finnOppgaveResponse,
                    )
                }
                throw ManglerOppgaveFeil("Fant ingen oppgave for behandling ${behandling.eksternBrukId} på fagsak ${fagsak.eksternFagsakId}. Oppgaven kan være manuelt lukket.")
            }

            else -> {
                return finnOppgaveResponse.oppgaver.first()
            }
        }
    }

    fun opprettOppgave(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
        enhet: String,
        beskrivelse: String?,
        fristForFerdigstillelse: LocalDate,
        saksbehandler: String?,
        prioritet: OppgavePrioritet,
        logContext: SecureLog.Context,
        behandlesAvApplikasjon: String? = "familie-tilbake",
        saksId: String? = behandling.eksternBrukId.toString(),
    ) {
        val fagsakId = behandling.fagsakId
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val aktørId = personService.hentAktivAktørId(fagsak.bruker.ident, fagsak.fagsystem, logContext)

        // Sjekk om oppgave allerede finnes for behandling
        val (_, finnOppgaveRespons) = finnOppgave(behandling, oppgavetype, fagsak)
        if (finnOppgaveRespons.oppgaver.isNotEmpty() && !finnesFerdigstillOppgaveForBehandling(behandling.id, oppgavetype)) {
            log.medContext(logContext) {
                info(
                    "Det finnes allerede en oppgave {} for behandling {} og finnes ikke noen ferdigstilleoppgaver. Eksisterende oppgaven {} må lukke først.",
                    oppgavetype,
                    behandling.id,
                    oppgavetype,
                )
            }
            return
        }

        val opprettOppgave =
            OpprettOppgaveRequest(
                ident =
                    OppgaveIdentV2(
                        ident = aktørId,
                        gruppe = IdentGruppe.AKTOERID,
                    ),
                saksId = saksId,
                tema = fagsak.ytelsestype.tilTema(),
                oppgavetype = oppgavetype,
                behandlesAvApplikasjon = behandlesAvApplikasjon,
                fristFerdigstillelse = fristForFerdigstillelse,
                beskrivelse =
                    lagOppgaveTekst(
                        fagsak.eksternFagsakId,
                        behandling.eksternBrukId.toString(),
                        fagsak.fagsystem.name,
                        beskrivelse,
                    ),
                enhetsnummer = behandling.behandlendeEnhet,
                tilordnetRessurs = saksbehandler,
                behandlingstype = Behandlingstype.Tilbakekreving.value,
                behandlingstema = null,
                mappeId = finnAktuellMappe(enhet, oppgavetype, logContext),
                prioritet = prioritet,
            )

        val oppgaveResponse = integrasjonerClient.opprettOppgave(opprettOppgave)
        antallOppgaveTyper[oppgavetype]!!.increment()
        log.medContext(logContext) {
            info("Ny oppgave (id={}, type={}, frist={}) opprettet for behandling {}", oppgaveResponse.oppgaveId, oppgavetype, fristForFerdigstillelse, behandling.id)
        }
    }

    fun hentOppgaveSomIkkeErFerdigstilt(
        oppgavetype: Oppgavetype,
        behandling: Behandling,
    ): Oppgave? {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val (_, finnOppgaveResponse) = finnOppgave(behandling, oppgavetype, fagsak)
        val alleOppgaver = finnOppgaveResponse.oppgaver
        val ikkeFerdigstilteOppgaver = alleOppgaver.filter { it.status != StatusEnum.FERDIGSTILT }
        if (ikkeFerdigstilteOppgaver.size > 1) {
            SecureLog.medContext(logContext) {
                warn(
                    "Fant flere enn en oppgave for behandling med fagsakId={}, oppgaveinfo: ({})",
                    behandling.fagsakId,
                    ikkeFerdigstilteOppgaver.joinToString(",") { "opprettet: ${it.opprettetTidspunkt}, type: ${it.oppgavetype}" },
                )
            }
        } else if (ikkeFerdigstilteOppgaver.isEmpty()) {
            SecureLog.medContext(logContext) {
                warn(
                    "Fant ingen fagsystemsaker som ikke er ferdigstilte fagsakId={}, oppgaveinfo: ({})",
                    behandling.fagsakId,
                    alleOppgaver.joinToString(",") { "opprettet: ${it.opprettetTidspunkt}, type: ${it.oppgavetype}, status: ${it.status}" },
                )
            }
        }
        return ikkeFerdigstilteOppgaver.singleOrNull()
    }

    private fun finnAktuellMappe(
        enhetsnummer: String?,
        oppgavetype: Oppgavetype,
        logContext: SecureLog.Context,
    ): Long? {
        if (enhetsnummer == NAY_ENSLIG_FORSØRGER) {
            val søkemønster = lagSøkeuttrykk(oppgavetype, logContext) ?: return null
            val mapper = integrasjonerClient.finnMapper(enhetsnummer)

            val mappeIdForOppgave = mapper.find { it.navn.matches(søkemønster) }?.id?.toLong()
            mappeIdForOppgave?.let {
                log.medContext(logContext) {
                    info("Legger oppgave i Godkjenne vedtak-mappe")
                }
            } ?: log.medContext(logContext) {
                error("Fant ikke mappe for oppgavetype = $oppgavetype")
            }

            return mappeIdForOppgave
        }
        return null
    }

    private fun lagSøkeuttrykk(
        oppgavetype: Oppgavetype,
        logContext: SecureLog.Context,
    ): Regex? {
        val pattern =
            when (oppgavetype) {
                Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak -> "50 Tilbakekreving?.+"
                Oppgavetype.GodkjenneVedtak -> "70 Godkjenne?.vedtak?.+"
                else -> {
                    log.medContext(logContext) {
                        error("Ukjent oppgavetype = $oppgavetype")
                    }
                    return null
                }
            }
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse = integrasjonerClient.patchOppgave(patchOppgave)

    fun tilordneOppgaveNyEnhet(
        oppgaveId: Long,
        nyEnhet: String,
        fjernMappeFraOppgave: Boolean,
        nullstillTilordnetRessurs: Boolean,
    ): OppgaveResponse = integrasjonerClient.tilordneOppgaveNyEnhet(oppgaveId, nyEnhet, fjernMappeFraOppgave, nullstillTilordnetRessurs)

    fun oppdaterEnhetOgSaksbehandler(
        behandlingId: UUID,
        enhetId: String,
        beskrivelse: String,
        logContext: SecureLog.Context,
        saksbehandler: String? = ContextService.hentSaksbehandler(logContext),
    ) {
        val oppgave = finnOppgaveForBehandlingUtenOppgaveType(behandlingId)

        val nyBeskrivelse =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm")) + ":" +
                beskrivelse + System.lineSeparator() + oppgave.beskrivelse
        var patchetOppgave = oppgave.copy(beskrivelse = nyBeskrivelse)
        if (!saksbehandler.isNullOrEmpty() && saksbehandler != Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            patchetOppgave = patchetOppgave.copy(tilordnetRessurs = saksbehandler)
        }

        patchOppgave(patchetOppgave)

        SecureLog.medContext(logContext) {
            info(
                "Oppdater enhet og saksbehandler for behandling {}, ",
                behandlingId,
            )
        }

        if (oppgave.tema == Tema.ENF) {
            tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, false, false) // ENF bruker generelle mapper
        } else if (oppgave.tema == Tema.BAR) {
            tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, true, true) // BAR bruker mapper som hører til enhetene og nullstiller tilordnetRessurs
        } else {
            tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, true, false) // KON bruker mapper som hører til enhetene
        }
    }

    fun ferdigstillOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype?,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val (finnOppgaveRequest, finnOppgaveResponse) = finnOppgave(behandling, oppgavetype, fagsak)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

        val tilbakekrevingsOppgaver = finnOppgaveResponse.oppgaver.filtrerTilbakekrevingsOppgave()

        when {
            tilbakekrevingsOppgaver.size > 1 -> {
                SecureLog.medContext(logContext) {
                    error(
                        "Mer enn en oppgave åpen for behandling {}, {}, {}",
                        behandling.eksternBrukId,
                        finnOppgaveRequest,
                        finnOppgaveResponse,
                    )
                }
                throw Feil(
                    message = "Har mer enn en åpen oppgave for behandling ${behandling.eksternBrukId}",
                    logContext = logContext,
                )
            }

            tilbakekrevingsOppgaver.isEmpty() -> {
                log.medContext(logContext) {
                    error("Fant ingen oppgave å ferdigstille for behandling ${behandling.eksternBrukId}")
                }
                SecureLog.medContext(logContext) {
                    error(
                        "Fant ingen oppgave å ferdigstille {}, {}, {}",
                        behandling.eksternBrukId,
                        finnOppgaveRequest,
                        finnOppgaveResponse,
                    )
                }
            }

            else -> {
                integrasjonerClient.ferdigstillOppgave(tilbakekrevingsOppgaver[0].id!!)
            }
        }
    }

    private fun List<Oppgave>.filtrerTilbakekrevingsOppgave(): List<Oppgave> =
        this.filter {
            it.oppgavetype in
                listOf(
                    Oppgavetype.BehandleSak.value,
                    Oppgavetype.GodkjenneVedtak.value,
                    Oppgavetype.BehandleUnderkjentVedtak.value,
                )
        }

    fun finnOppgave(
        behandling: Behandling,
        oppgavetype: Oppgavetype?,
        fagsak: Fagsak,
    ): Pair<FinnOppgaveRequest, FinnOppgaveResponseDto> {
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                saksreferanse = behandling.eksternBrukId.toString(),
                oppgavetype = oppgavetype,
                tema = fagsak.ytelsestype.tilTema(),
            )
        val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
        return Pair(finnOppgaveRequest, finnOppgaveResponse)
    }

    private fun lagOppgaveTekst(
        eksternFagsakId: String,
        eksternbrukBehandlingID: String,
        fagsystem: String,
        beskrivelse: String? = null,
    ): String =
        if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } + "--- Opprettet av tilbakekreving ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} ---\n" +
            "$frontendUrl/fagsystem/$fagsystem/fagsak/$eksternFagsakId/behandling/" +
            eksternbrukBehandlingID

    private fun finnesFerdigstillOppgaveForBehandling(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): Boolean {
        val ubehandledeTasker =
            taskService.finnTasksMedStatus(
                status =
                    listOf(
                        Status.UBEHANDLET,
                        Status.PLUKKET,
                        Status.FEILET,
                        Status.KLAR_TIL_PLUKK,
                        Status.BEHANDLER,
                    ),
                type = FerdigstillOppgaveTask.TYPE,
                page = Pageable.unpaged(),
            )
        return ubehandledeTasker.any {
            it.payload == behandlingId.toString() &&
                it.metadata.getProperty("oppgavetype") == oppgavetype.name
        }
    }

    companion object {
        private const val NAY_ENSLIG_FORSØRGER = "4489"
        private const val NAY_EGNE_ANSATTE = "4483"
    }
}
