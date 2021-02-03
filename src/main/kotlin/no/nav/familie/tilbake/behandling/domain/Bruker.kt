package no.nav.familie.tilbake.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class Bruker(val ident: String,
                  @Column("sprakkode")
                  val språkkode: String? = "NB") {

    companion object {

        fun velgSpråkkode(kode: String?): String {
            return when (kode) {
                "NB" -> kode
                "NN" -> kode
                else -> "NB"
            }
        }
    }
}
