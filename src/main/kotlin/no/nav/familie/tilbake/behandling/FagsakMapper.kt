package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.tilbake.api.dto.BehandlingsoppsummeringDto
import no.nav.familie.tilbake.api.dto.BrukerDto
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo

object FagsakMapper {

    fun tilRespons(fagsak: Fagsak,
                   personInfo: PersonInfo,
                   behandlinger: List<Behandling>): FagsakDto {
        val bruker = BrukerDto(personIdent = PersonIdent(fagsak.bruker.ident),
                               navn = personInfo.navn,
                               fødselsdato = personInfo.fødselsdato,
                               kjønn = personInfo.kjønn)

        val behandlingListe = behandlinger.map { behandling ->
            BehandlingsoppsummeringDto(behandlingId = behandling.id,
                                       eksternBrukId = behandling.eksternBrukId,
                                       type = behandling.type,
                                       status = behandling.status)
        }.toSet()
        return FagsakDto(eksternFagsakId = fagsak.eksternFagsakId,
                         status = fagsak.status,
                         ytelsestype = fagsak.ytelsestype,
                         språkkode = fagsak.bruker.språkkode!!,
                         bruker = bruker,
                         behandlinger = behandlingListe)
    }
}
