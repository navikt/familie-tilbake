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
                  val sporbar: Sporbar = Sporbar()) {

    val ytelsesnavn get() =  ytelsestype.navn[bruker.språkkode]
                             ?: throw IllegalStateException("Programmeringsfeil: Sprækkode lagt til uten støtte")
}

enum class Ytelsestype(val kode: String, val navn: Map<Språkkode, String>) {
    BARNETRYGD("BA", mapOf(Språkkode.NB to "Barnetrygd", Språkkode.NN to "Barnetrygd")),
    OVERGANGSSTØNAD("OG", mapOf(Språkkode.NB to "Overgangsstønad", Språkkode.NN to "Overgangsstønad")),
    BARNETILSYN("BT", mapOf(Språkkode.NB to "Stønad til barnetilsyn", Språkkode.NN to "Stønad til barnetilsyn")),
    SKOLEPENGER("SP", mapOf(Språkkode.NB to "Stønad til skolepenger", Språkkode.NN to "Stønad til skulepengar")),
    KONTANTSTØTTE("KS", mapOf(Språkkode.NB to "Kontantstøtte", Språkkode.NN to "Kontantstøtte"));

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
    BA("BA", "BAR"),
    EF("EF", "ENF"),
    KS("KS", "KON"),
    SYSTEM_TILGANG("", ""); //brukes internt bare for tilgangsskontroll
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
