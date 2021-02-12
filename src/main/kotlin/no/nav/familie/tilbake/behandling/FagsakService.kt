package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.api.dto.FagsakResponsDto
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
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
    fun hentFagsak(ytelsestype: Ytelsestype, eksternFagsakId: String): FagsakResponsDto {
        val fagsak = fagsakRepository.findByYtelsestypeAndEksternFagsakId(ytelsestype = ytelsestype,
                                                                          eksternFagsakId = eksternFagsakId)
                     ?: throw Feil(
                             message = "Fagsak finnes ikke for $ytelsestype og $eksternFagsakId",
                             frontendFeilmelding = "Fagsak finnes ikke for $ytelsestype og $eksternFagsakId",
                             httpStatus = HttpStatus.BAD_REQUEST
                     )
        val personInfo = personService.hentPersoninfo(personIdent = fagsak.bruker.ident,
                                                      fagsystem = fagsak.fagsystem)
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        return FagsakMapper.tilRespons(fagsak = fagsak,
                                       personInfo = personInfo,
                                       behandlinger = behandlinger)
    }
}
