package no.nav.familie.tilbake.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class Bruker(val ident: String?,
                  @Column("sprakkode")
                  val språkkode: String? = "NB")
