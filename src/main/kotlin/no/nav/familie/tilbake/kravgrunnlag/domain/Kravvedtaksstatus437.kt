package no.nav.familie.tilbake.kravgrunnlag.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Kravvedtaksstatus437(@Id
                                val id: UUID = UUID.randomUUID(),
                                val vedtakId: String,
                                val kravstatuskode: Kravstatuskode,
                                @Column("fagomradekode")
                                val fagområdekode: Fagområdekode,
                                val fagsystemId: String,
                                val gjelderVedtakId: String,
                                val gjelderType: GjelderType,
                                val referanse: String?,
                                @Version
                                val versjon: Long = 0,
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())

enum class Kravstatuskode(@JsonValue val kode: String, val navn: String) {

    ANNULERT("ANNU", "Kravgrunnlag annullert"),
    ANNULLERT_OMG("ANOM", "Kravgrunnlag annullert ved omg"),
    AVSLUTTET("AVSL", "Avsluttet kravgrunnlag"),
    BEHANDLET("BEHA", "Kravgrunnlag ferdigbehandlet"),
    ENDRET("ENDR", "Endret kravgrunnlag"),
    FEIL("FEIL", "Feil på kravgrunnlag"),
    MANUELL("MANU", "Manuell behandling"),
    NYTT("NY", "Nytt kravgrunnlag"),
    SPERRET("SPER", "Kravgrunnlag sperret");

    companion object {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fraKode(kode: String): Kravstatuskode {
            for (kravstatuskode in values()) {
                if (kode == kravstatuskode.kode) {
                    return kravstatuskode
                }
            }
            throw IllegalArgumentException("Kravstatuskode finnes ikke for kode $kode")
        }
    }
}
