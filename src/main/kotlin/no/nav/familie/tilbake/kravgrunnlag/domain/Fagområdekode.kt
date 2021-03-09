package no.nav.familie.tilbake.kravgrunnlag.domain


enum class Fagområdekode(val navn: String) {

    BA("Barnetrygd"),
    KS("Kontantstøtte"),
    EFOG("Enslig forelder - Overgangsstønad"),
    EFBT("Enslig forelder - Barnetilsyn"),
    EFSP("Enslig forelder - Skolepenger");

    companion object {

        fun fraKode(kode: String): Fagområdekode {
            for (fagområdekode in values()) {
                if (fagområdekode.name == kode) {
                    return fagområdekode
                }
            }
            throw IllegalArgumentException("Ukjent Fagområdekode $kode")
        }
    }

}
