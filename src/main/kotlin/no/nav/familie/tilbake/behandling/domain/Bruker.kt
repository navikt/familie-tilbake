package no.nav.familie.tilbake.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class Bruker(val ident: String?,
                  @Column("sprakkode")
                  val språkkode: String? = "NB") {

    companion object {

        fun velgSpråkkode(kode: String?): String {
            return when {
                kode?.equals("NB") == true -> kode
                kode?.equals("NN") == true -> kode
                else -> "NB"
            }
        }
    }
}
