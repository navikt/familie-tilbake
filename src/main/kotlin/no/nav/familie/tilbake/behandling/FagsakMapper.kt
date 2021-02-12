package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.api.dto.BehandlingsoppsummeringDto
import no.nav.familie.tilbake.api.dto.BrukerDto
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.api.dto.FagsakResponsDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo

object FagsakMapper {

    fun tilRespons(fagsak: Fagsak,
                   personInfo: PersonInfo,
                   behandlinger: List<Behandling>): FagsakResponsDto {
        val bruker = BrukerDto(søkerFødselsnummer = fagsak.bruker.ident,
                               navn = personInfo.navn,
                               fødselsdato = personInfo.fødselsdato,
                               kjønn = personInfo.kjønn)
        val fagsakDto = FagsakDto(eksternFagsakId = fagsak.eksternFagsakId,
                                  status = fagsak.status,
                                  ytelsestype = fagsak.ytelsestype,
                                  språkkode = fagsak.bruker.språkkode!!,
                                  bruker = bruker)

        val behandlingskontekst = behandlinger.map { behandling ->
            BehandlingsoppsummeringDto(behandlingId = behandling.id,
                                       eksternBrukId = behandling.eksternBrukId,
                                       type = behandling.type,
                                       status = behandling.status)
        }.toSet()
        return FagsakResponsDto(fagsak = fagsakDto,
                                behandlinger = behandlingskontekst)
    }
}
