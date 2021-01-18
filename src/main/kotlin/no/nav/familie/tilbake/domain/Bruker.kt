package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Bruker(@Id
                  val id: UUID = UUID.randomUUID(),
                  val ident: String?,
                  @Column("sprakkode")
                  val språkkode: String = "NB",
                  val versjon: Int = 0,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())