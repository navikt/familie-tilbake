package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import java.time.LocalDate
import java.util.UUID

data class BrevEntity(
    val brevType: Brevtype,
    val internId: UUID,
    val opprettetDato: LocalDate,
    val varsletBeløp: Long?,
) {
    fun fraEntity(): Brev {
        return when (brevType) {
            Brevtype.VARSEL_BREV -> Varselbrev(
                requireNotNull(internId) { "internId kreves for Brev" },
                requireNotNull(opprettetDato) { "opprettetDato kreves for Brev" },
                requireNotNull(varsletBeløp) { "varsletBeløp kreves for Brev" },
            )
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
