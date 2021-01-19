package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Kravgrunnlagsperiode432(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   val kravgrunnlag431Id: UUID,
                                   val fom: LocalDate,
                                   val tom: LocalDate,
                                   @Column("manedlig_skattebelop")
                                   val månedligSkattebeløp: Double,
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())