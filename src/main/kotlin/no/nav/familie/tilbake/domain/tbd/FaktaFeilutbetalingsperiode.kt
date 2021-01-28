package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
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
    BA_ANNET("Annet", 999),
    EF_ANNET("Annet", 999),
    KS_ANNET("Annet", 999)
}

enum class Hendelsesundertype(val navn: String?, val sortering: Int) {
    IKKE_INNTEKT("Ikke inntekt 6 av siste 10 måneder", 0),
    IKKE_YRKESAKTIV("Ikke yrkesaktiv med pensjonsgivende inntekt", 1),
    INNTEKT_UNDER("Inntekt under 1/2 G", 1),
    ENDRING_GRUNNLAG("Endring i selve grunnlaget", 0),
    ENDRET_DEKNINGSGRAD("Endret dekningsgrad", 0),
}
