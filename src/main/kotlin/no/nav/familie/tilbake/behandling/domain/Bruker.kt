package no.nav.familie.tilbake.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class Bruker(val ident: String,
                  @Column("sprakkode")
                  val språkkode: Språkkode = Språkkode.NB) {

    companion object {

        fun velgSpråkkode(kode: String?): Språkkode {
            return when (kode) {
                "NB" -> Språkkode.NB
                "NN" -> Språkkode.NN
                else -> Språkkode.NB
            }
        }
    }
}

enum class Språkkode {
    NB,
    NN;
}
