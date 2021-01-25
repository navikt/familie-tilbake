package no.nav.familie.tilbake.varsel

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

data class Varselsperiode(@Id
                          val id: UUID = UUID.randomUUID(),
                          val fom: LocalDate,
                          val tom: LocalDate,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())
