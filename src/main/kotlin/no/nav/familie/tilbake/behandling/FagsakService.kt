package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.api.dto.FagsakDto
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
}
