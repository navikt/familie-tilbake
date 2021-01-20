package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Foreldelsesperiode(@Id
                              val id: UUID = UUID.randomUUID(),
                              val vurdertForeldelseId: UUID,
                              val fom: LocalDate,
                              val tom: LocalDate,
                              val foreldelsesvurderingstype: Foreldelsesvurderingstype,
                              val begrunnelse: String,
                              val foreldelsesfrist: LocalDate?,
                              val oppdagelsesdato: LocalDate?,
                              @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                              val sporbar: Sporbar = Sporbar())

enum class Foreldelsesvurderingstype(val navn: String) {
    IKKE_VURDERT("Perioden er ikke vurdert"),
    FORELDET("Perioden er foreldet"),
    IKKE_FORELDET("Perioden er ikke foreldet"),
    TILLEGGSFRIST("Perioden er ikke foreldet, regel om tilleggsfrist (10 år) benyttes"),
    UDEFINERT("Ikke Definert")
}
