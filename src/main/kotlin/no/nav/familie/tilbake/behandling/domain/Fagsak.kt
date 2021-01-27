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
                  val ytelsestype: String,
                  val status: Fagsaksstatus = Fagsaksstatus.OPPRETTET,
                  @Version
                  val versjon: Int = 0,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

enum class Ytelsestype(val kode: String) {
    BARNETRYGD("BA"),
    OVERGANGSSTØNAD("OG"),
    BARNETILSYN("BT"),
    SKOLEPENGER("SP"),
    KONTANTSTØTTE("KS");

    companion object {

        fun fraKode(kode: String): Ytelsestype {
            for (ytelsestype in values()) {
                if (ytelsestype.kode == kode) {
                    return ytelsestype
                }
            }
            throw IllegalArgumentException("Ytelsestype finnes ikke for kode $kode")
        }
    }
}

enum class Fagsystem {
    BA,
    EF,
    KS;

    companion object {

        fun fraYtelsestype(type: Ytelsestype): Fagsystem {
            return when (type) {
                Ytelsestype.BARNETRYGD -> BA
                Ytelsestype.KONTANTSTØTTE -> KS
                Ytelsestype.OVERGANGSSTØNAD -> EF
                Ytelsestype.BARNETILSYN -> EF
                Ytelsestype.SKOLEPENGER -> EF
            }
        }
    }
}

enum class Fagsaksstatus(val kode: String) {
    OPPRETTET("OPPR"),
    UNDER_BEHANDLING("UBEH"),
    AVSLUTTET("AVSLU");
}
