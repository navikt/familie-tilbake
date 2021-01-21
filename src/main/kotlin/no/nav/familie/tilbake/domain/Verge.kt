package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Verge(@Id
                 val id: UUID = UUID.randomUUID(),
                 val ident: String?,
                 val orgNr: String?,
                 val gyldigFom: LocalDate,
                 val gyldigTom: LocalDate,
                 val aktiv: Boolean = true,
                 val type: Vergetype,
                 val navn: String,
                 val kilde: String,
                 val begrunnelse: String?,
                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                 val sporbar: Sporbar = Sporbar())

enum class Vergetype(val navn: String) {
    BARN("Verge for barn under 18 år"),
    FBARN("Verge for foreldreløst barn under 18 år"),
    VOKSEN("Verge for voksen"),
    ADVOKAT("Advokat/advokatfullmektig"),
    ANNEN_F("Annen fullmektig"),
    UDEFINERT("UDefinert");
}
