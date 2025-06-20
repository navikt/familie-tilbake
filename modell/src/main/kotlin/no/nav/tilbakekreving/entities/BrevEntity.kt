package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import java.time.LocalDate
import java.util.UUID

data class BrevEntity(
    val brevType: Brevtype,
    val internId: UUID,
    val opprettetDato: LocalDate,
    val varsletBeløp: Long? = null,
) {
    fun fraEntity(): Brev {
        return when (brevType) {
            Brevtype.VARSEL_BREV -> Varselbrev(internId, opprettetDato, varsletBeløp!!)
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
