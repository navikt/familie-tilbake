package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  @Embedded(prefix = "bruker_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val bruker: Bruker,
                  val eksternFagsakId: String?,
                  val ytelsestype: Ytelsestype,
                  val status: Fagsaksstatus = Fagsaksstatus.OPPRETTET,
                  val versjon: Int = 0,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

enum class Ytelsestype {
    BA,
    OG,
    BT,
    UT,
    KS
}

enum class Fagsaksstatus(val kode: String) {
    OPPRETTET("OPPR"),
    UNDER_BEHANDLING("UBEH"),
    AVSLUTTET("AVSLU");
}