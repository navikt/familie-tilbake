package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.api.dto.BehandlingsoppsummeringDto
import no.nav.familie.tilbake.api.dto.BrukerDto
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo

object FagsakMapper {

    fun tilRespons(fagsak: Fagsak,
                   personinfo: Personinfo,
                   behandlinger: List<Behandling>): FagsakDto {
        val bruker = BrukerDto(personIdent = fagsak.bruker.ident,
                               navn = personinfo.navn,
                               fødselsdato = personinfo.fødselsdato,
                               kjønn = personinfo.kjønn)

        val behandlingListe = behandlinger.map {
            BehandlingsoppsummeringDto(behandlingId = it.id,
                                       eksternBrukId = it.eksternBrukId,
                                       type = it.type,
                                       status = it.status)
        }.toSet()
        return FagsakDto(eksternFagsakId = fagsak.eksternFagsakId,
                         status = fagsak.status,
                         ytelsestype = fagsak.ytelsestype,
                         fagsystem = fagsak.fagsystem,
                         språkkode = fagsak.bruker.språkkode,
            bruker = bruker,
                         behandlinger = behandlingListe)
    }
}
