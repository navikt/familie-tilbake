package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import java.time.LocalDate
import java.util.UUID

class Varselbrev(
    override val internId: UUID,
    override val opprettetDato: LocalDate,
    val varsletBeløp: Long,
) : Brev {
    companion object {
        fun opprett(
            varsletBeløp: Long,
        ): Brev {
            return Varselbrev(
                internId = UUID.randomUUID(),
                varsletBeløp = varsletBeløp,
                opprettetDato = LocalDate.now(),
            )
        }
    }

    override fun tilEntity(): BrevEntity {
        return BrevEntity(
            brevType = "VARSEL_BREV",
            internId = internId,
            opprettetDato = opprettetDato,
            varsletBeløp = varsletBeløp,
        )
    }
}
