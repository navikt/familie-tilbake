package no.nav.familie.tilbake.verge

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Verge(@Id
                 val id: UUID = UUID.randomUUID(),
                 val ident: String? = null,
                 val orgNr: String? = null,
                 val gyldigFom: LocalDate,
                 val gyldigTom: LocalDate,
                 val aktiv: Boolean = true,
                 val type: Vergetype,
                 val navn: String,
                 val kilde: String,
                 val begrunnelse: String? = "",
                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                 val sporbar: Sporbar = Sporbar())

enum class Vergetype(val navn: String) {
    BARN("Verge for barn under 18 år"),
    FORELDRELØST_BARN("Verge for foreldreløst barn under 18 år"),
    VOKSEN("Verge for voksen"),
    ADVOKAT("Advokat/advokatfullmektig"),
    ANNEN_FULLMEKTIG("Annen fullmektig"),
    UDEFINERT("UDefinert")
}
