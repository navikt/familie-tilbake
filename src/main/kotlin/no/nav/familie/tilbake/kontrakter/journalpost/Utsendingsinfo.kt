package no.nav.familie.tilbake.kontrakter.journalpost

import no.nav.familie.tilbake.kontrakter.journalpost.Utsendingsmåte.DIGITAL_POST
import no.nav.familie.tilbake.kontrakter.journalpost.Utsendingsmåte.FYSISK_POST
import java.time.LocalDateTime

data class Utsendingsinfo(
    val varselSendt: List<VarselSendt> = emptyList(),
    val fysiskpostSendt: FysiskpostSendt?,
    val digitalpostSendt: DigitalpostSendt?,
) {
    val utsendingsmåter =
        Utsendingsmåte.values().filter {
            when (it) {
                FYSISK_POST -> fysiskpostSendt != null
                DIGITAL_POST -> digitalpostSendt != null
            }
        }
}

data class VarselSendt(
    val type: VarselType,
    val varslingstidspunkt: LocalDateTime?,
)

data class FysiskpostSendt(
    val adressetekstKonvolutt: String,
)

data class DigitalpostSendt(
    val adresse: String,
)

enum class VarselType {
    SMS,
    EPOST,
}

enum class Utsendingsmåte {
    FYSISK_POST,
    DIGITAL_POST,
}
