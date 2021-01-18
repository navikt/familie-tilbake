package no.nav.familie.tilbake.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded

data class Bruker(val ident: String?,
                  @Column("sprakkode")
                  val språkkode: String = "NB")