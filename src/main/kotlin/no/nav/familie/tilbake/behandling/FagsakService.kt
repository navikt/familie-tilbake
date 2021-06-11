package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.api.dto.FinnesBehandlingsresponsDto
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.person.PersonService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(val fagsakRepository: FagsakRepository,
                    val behandlingRepository: BehandlingRepository,
                    val personService: PersonService) {

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
        var finneÅpenBehandling = false
        if (fagsak != null) {
            finneÅpenBehandling =
                    behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype = fagsak.ytelsestype,
                                                                           eksternFagsakId = eksternFagsakId) != null
        }
        return FinnesBehandlingsresponsDto(finnesÅpenBehandling = finneÅpenBehandling)
    }

    @Transactional(readOnly = true)
    fun hentBehandlingerForFagsak(fagsystem: Fagsystem,
                                  eksternFagsakId: String): List<no.nav.familie.kontrakter.felles.tilbakekreving.Behandling> {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem = fagsystem,
                                                                        eksternFagsakId = eksternFagsakId)

        return if (fagsak != null) {
            val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)
            behandlinger.map { BehandlingMapper.tilBehandlingerForFagsystem(it) }
        } else {
            emptyList();
        }
    }
}
