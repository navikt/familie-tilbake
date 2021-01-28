package no.nav.familie.tilbake.domain.tbd

import org.springframework.data.relational.core.mapping.Column

data class Bruker(val ident: String?,
                  @Column("sprakkode")
                  val språkkode: Språkkode = Språkkode.NB)

enum class Språkkode {
    NB,
    NN,
    EN,
    UDEFINERT;
}
