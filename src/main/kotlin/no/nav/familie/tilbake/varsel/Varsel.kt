package no.nav.familie.tilbake.varsel

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.*
import javax.persistence.Version

data class Varsel(@Id
                  val id: UUID = UUID.randomUUID(),
                  val varseltekst: String,
                  @Column("varselbelop")
                  val varselbeløp: Long,
                  val revurderingsvedtaksdato: LocalDate,
                  @MappedCollection(idColumn = "varsel_id")
                  val perioder: Set<Varselsperiode> = setOf(),
                  val aktiv: Boolean = true,
                  @Version
                  val versjon: Int = 0,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())
