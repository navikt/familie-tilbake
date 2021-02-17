package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  @Embedded(prefix = "bruker_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val bruker: Bruker,
                  val eksternFagsakId: String,
                  val fagsystem: Fagsystem,
                  val ytelsestype: Ytelsestype,
                  val status: Fagsaksstatus = Fagsaksstatus.OPPRETTET,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())


enum class Fagsaksstatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    AVSLUTTET;
}
