package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.tilbake.kontrakter.dokdist.ManuellAdresse

class Adresseinfo(
    val ident: String,
    val mottagernavn: String,
    val annenMottagersNavn: String? = null,
    val manuellAdresse: ManuellAdresse? = null,
)
