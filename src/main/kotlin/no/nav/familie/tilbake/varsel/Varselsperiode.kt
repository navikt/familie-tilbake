package no.nav.familie.tilbake.varsel

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*
import javax.persistence.Version

data class Varselsperiode(@Id
                          val id: UUID = UUID.randomUUID(),
                          val fom: LocalDate,
                          val tom: LocalDate,
                          @Version
                          val versjon: Int = 0,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())
