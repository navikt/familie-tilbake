package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

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
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())

enum class Kravstatuskode(val navn: String) {
    ANNULERT("Kravgrunnlag annullert"),
    ANNULLERT_OMG("Kravgrunnlag annullert ved omg"),
    AVSLUTTET("Avsluttet kravgrunnlag"),
    BEHANDLET("Kravgrunnlag ferdigbehandlet"),
    ENDRET("Endret kravgrunnlag"),
    FEIL("Feil på kravgrunnlag"),
    MANUELL("Manuell behandling"),
    NYTT("Nytt kravgrunnlag"),
    SPERRET("Kravgrunnlag sperret");
}
