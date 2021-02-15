package no.nav.familie.tilbake.behandling.domain

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

enum class Fagsystem(val kode: String, val tema: String) {
    BARNETRYGD("BA", "BAR"),
    ENSLIG_FORELDER("EF", "ENF"),
    KONTANTSTØTTE("KS", "KON"),
    SYSTEM_TILGANG("", ""); //brukes internt bare for tilgangsskontroll
    companion object {

        fun fraYtelsestype(type: Ytelsestype): Fagsystem {
            return when (type) {
                Ytelsestype.BARNETRYGD -> BARNETRYGD
                Ytelsestype.KONTANTSTØTTE -> KONTANTSTØTTE
                Ytelsestype.OVERGANGSSTØNAD -> ENSLIG_FORELDER
                Ytelsestype.BARNETILSYN -> ENSLIG_FORELDER
                Ytelsestype.SKOLEPENGER -> ENSLIG_FORELDER
            }
        }

       fun fraKode(kode: String): Fagsystem  {
           for (fagsystem in values()) {
               if (fagsystem.kode == kode) {
                   return fagsystem
               }
           }
           throw IllegalArgumentException("Fagsystem finnes ikke for kode $kode")
       }
    }
}

enum class Fagsaksstatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    AVSLUTTET;
}
