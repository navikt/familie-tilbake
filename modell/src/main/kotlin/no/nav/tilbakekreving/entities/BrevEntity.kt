package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import java.time.LocalDate
import java.util.UUID

data class BrevEntity(
    val brevType: String,
    val internId: String,
    val opprettetDato: LocalDate,
    val varsletBeløp: Long? = null,
) {
    fun fraEntity(): Brev {
        val brev = when {
            brevType.equals("VARSEL_BREV") -> Varselbrev(UUID.fromString(internId), opprettetDato, varsletBeløp!!)
            else -> throw IllegalArgumentException("Ugyldig brevType=$brevType")
        }
        return brev
    }
}
