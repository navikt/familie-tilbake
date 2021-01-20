package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class FaktaFeilutbetalingsperiode(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       val faktaFeilutbetalingId: UUID?,
                                       val fom: LocalDate,
                                       val tom: LocalDate,
                                       val hendelsestype: Hendelsestype,
                                       val hendelsesundertype: Hendelsesundertype,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())

enum class Hendelsestype(val navn: String, val sortering: Int) {
    MEDLEMSKAP_TYPE("§14-2 Medlemskap", 10),
    ØKONOMI_FEIL("Feil i økonomi", 500),
    PSB_ANNET_TYPE("Annet", 999),
    PPN_ANNET_TYPE("Annet", 999),
    OLP_ANNET_TYPE("Annet", 999)
}

enum class Hendelsesundertype(val navn: String?, val sortering: Int) {
    IKKE_INNTEKT("Ikke inntekt 6 av siste 10 måneder", 0),
    IKKE_YRKESAKTIV("Ikke yrkesaktiv med pensjonsgivende inntekt", 1),
    INNTEKT_UNDER("Inntekt under 1/2 G", 1),
    ENDRING_GRUNNLAG("Endring i selve grunnlaget", 0),
    ENDRET_DEKNINGSGRAD("Endret dekningsgrad", 0),
}
