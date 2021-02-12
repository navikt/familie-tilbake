package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import java.time.LocalDate
import java.util.UUID

data class FagsakDto(val eksternFagsakId: String,
                     val status: Fagsaksstatus,
                     val ytelsestype: Ytelsestype,
                     val språkkode: String,
                     val bruker: BrukerDto)

data class BrukerDto(val søkerFødselsnummer: String,
                     val navn: String,
                     val fødselsdato: LocalDate,
                     val kjønn: Kjønn)

data class BehandlingsoppsummeringDto(val behandlingId: UUID,
                                      val eksternBrukId: UUID,
                                      val type: Behandlingstype,
                                      val status: Behandlingsstatus)

data class FagsakResponsDto(val fagsak: FagsakDto,
                            val behandlinger: Set<BehandlingsoppsummeringDto> = setOf())
