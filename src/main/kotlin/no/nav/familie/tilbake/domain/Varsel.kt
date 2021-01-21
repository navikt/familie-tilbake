package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Varsel(@Id
                  val id: UUID = UUID.randomUUID(),
                  val varseltekst: String,
                  @Column("varselbelop")
                  val varselbeløp: Long?,
                  val aktiv: Boolean = true,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())