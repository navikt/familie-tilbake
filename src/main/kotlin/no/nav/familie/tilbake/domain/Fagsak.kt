package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  val brukerId: UUID,
                  val eksternFagsakId: String?,
                  val fagsakstatus: String,
                  val versjon: Int = 0,
                  val ytelsestype: Ytelsestype = Ytelsestype.BA,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

enum class Ytelsestype {
    BA,
    OG,
    BT,
    UT,
    KS
}