package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*
import javax.persistence.Version

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  @Embedded(prefix = "bruker_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val bruker: Bruker,
                  val eksternFagsakId: String?,
                  val fagsystem: Fagsystem,
                  val ytelsestype: Ytelsestype,
                  val status: Fagsaksstatus = Fagsaksstatus.OPPRETTET,
                  @Version
                  val versjon: Int = 0,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

enum class Ytelsestype(val beskrivelse: String) {
    BA("Barnetrygd"),
    OG("Overgangsstønad"),
    BT("Barnetilsyn"),
    SP("Skolepenger"),
    KS("Konstantsstøtte")
}

enum class Fagsystem {
    BA,
    EF,
    KS;

    companion object {

        fun fraYtelsestype(type: Ytelsestype): Fagsystem {
            return when (type) {
                Ytelsestype.BA -> BA
                Ytelsestype.KS -> KS
                Ytelsestype.OG -> EF
                Ytelsestype.BT -> EF
                Ytelsestype.SP -> EF
            }
        }
    }
}

enum class Fagsaksstatus(val kode: String) {
    OPPRETTET("OPPR"),
    UNDER_BEHANDLING("UBEH"),
    AVSLUTTET("AVSLU");
}
