package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.kontrakter.bruker.FrontendBrukerDto
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn

object FagsakMapper {
    fun tilRespons(
        fagsak: Fagsak,
        personinfo: Personinfo,
        behandlinger: List<Behandling>,
        organisasjonService: OrganisasjonService,
    ): FagsakDto {
        val bruker =
            FrontendBrukerDto(
                personIdent = fagsak.bruker.ident,
                navn = personinfo.navn,
                fødselsdato = personinfo.fødselsdato,
                kjønn =
                    when (personinfo.kjønn) {
                        PdlKjønnType.MANN -> Kjønn.MANN
                        PdlKjønnType.KVINNE -> Kjønn.KVINNE
                        PdlKjønnType.UKJENT -> Kjønn.UKJENT
                    },
                dødsdato = personinfo.dødsdato,
            )

        val behandlingListe =
            behandlinger.map {
                BehandlingsoppsummeringDto(
                    behandlingId = it.id,
                    eksternBrukId = it.eksternBrukId,
                    type = it.type,
                    status = it.status,
                )
            }

        val institusjon =
            fagsak.institusjon?.let {
                organisasjonService.mapTilInstitusjonDto(orgnummer = it.organisasjonsnummer)
            }

        return FagsakDto(
            eksternFagsakId = fagsak.eksternFagsakId,
            ytelsestype = fagsak.ytelsestype.tilDTO(),
            fagsystem = fagsak.fagsystem.tilDTO(),
            språkkode = fagsak.bruker.språkkode,
            bruker = bruker,
            behandlinger = behandlingListe,
            institusjon = institusjon,
        )
    }
}
