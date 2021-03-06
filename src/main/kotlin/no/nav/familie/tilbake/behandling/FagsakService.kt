package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.api.dto.FinnesBehandlingsresponsDto
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.person.PersonService
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val behandlingRepository: BehandlingRepository,
                    private val taskRepository: TaskRepository,
                    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
                    private val personService: PersonService) {

    @Transactional(readOnly = true)
    fun hentFagsak(fagsystem: Fagsystem, eksternFagsakId: String): FagsakDto {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem = fagsystem,
                                                                        eksternFagsakId = eksternFagsakId)
                     ?: throw Feil(message = "Fagsak finnes ikke for ${fagsystem.navn} og $eksternFagsakId",
                                   frontendFeilmelding = "Fagsak finnes ikke for ${fagsystem.navn} og $eksternFagsakId",
                                   httpStatus = HttpStatus.BAD_REQUEST)
        val personInfo = personService.hentPersoninfo(personIdent = fagsak.bruker.ident,
                                                      fagsystem = fagsak.fagsystem)
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        return FagsakMapper.tilRespons(fagsak = fagsak,
                                       personinfo = personInfo,
                                       behandlinger = behandlinger)
    }

    @Transactional(readOnly = true)
    fun finnesÅpenTilbakekrevingsbehandling(fagsystem: Fagsystem, eksternFagsakId: String): FinnesBehandlingsresponsDto {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem = fagsystem,
                                                                        eksternFagsakId = eksternFagsakId)
        var finnesÅpenBehandling = false
        if (fagsak != null) {
            finnesÅpenBehandling =
                    behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype = fagsak.ytelsestype,
                                                                           eksternFagsakId = eksternFagsakId) != null
        }
        return FinnesBehandlingsresponsDto(finnesÅpenBehandling = finnesÅpenBehandling)
    }

    @Transactional(readOnly = true)
    fun hentBehandlingerForFagsak(fagsystem: Fagsystem,
                                  eksternFagsakId: String): List<no.nav.familie.kontrakter.felles.tilbakekreving.Behandling> {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem = fagsystem,
                                                                        eksternFagsakId = eksternFagsakId)

        return fagsak?.let {
            val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)
            behandlinger.map { BehandlingMapper.tilBehandlingerForFagsystem(it) }
        } ?: emptyList()
    }

    @Transactional(readOnly = true)
    fun kanBehandlingOpprettesManuelt(eksternFagsakId: String,
                                      ytelsestype: Ytelsestype): KanBehandlingOpprettesManueltRespons {
        val finnesÅpenTilbakekreving: Boolean =
                behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId) != null
        if (finnesÅpenTilbakekreving) {
            return KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = false,
                                                        melding = "Det finnes allerede en åpen tilbakekrevingsbehandling. " +
                                                                  "Den ligger i saksoversikten.")
        }
        val kravgrunnlagene = økonomiXmlMottattRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
        if (kravgrunnlagene.isEmpty()) {
            return KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = false,
                                                        melding = "Det finnes ingen feilutbetaling på saken, så du kan " +
                                                                  "ikke opprette tilbakekrevingsbehandling.")
        }
        val kravgrunnlagsreferanse = kravgrunnlagene.first().referanse
        val harAlledeMottattForespørselen: Boolean =
                taskRepository.findByStatusIn(listOf(Status.UBEHANDLET, Status.BEHANDLER,
                                                     Status.KLAR_TIL_PLUKK, Status.PLUKKET,
                                                     Status.FEILET), Pageable.unpaged())
                        .any {
                            OpprettBehandlingManueltTask.TYPE == it.type &&
                            eksternFagsakId == it.metadata.getProperty("eksternFagsakId") &&
                            ytelsestype.kode == it.metadata.getProperty("ytelsestype")
                            kravgrunnlagsreferanse == it.metadata.getProperty("eksternId")
                        }

        if (harAlledeMottattForespørselen) {
            return KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = false,
                                                        melding = "Det finnes allerede en forespørsel om å opprette tilbakekrevingsbehandling. " +
                                                                  "Behandlingen vil snart bli tilgjengelig i saksoversikten. Dersom " +
                                                                  "den ikke dukker opp, ta kontakt brukerstøtte for å rapportere feilen.")
        }
        return KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = true,
                                                    kravgrunnlagsreferanse = kravgrunnlagsreferanse,
                                                    melding = "Det er mulig å opprette behandling manuelt.")
    }

}
