package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import java.time.LocalDate
import java.util.UUID

class Varselbrev(
    override val id: UUID,
    override val opprettetDato: LocalDate,
    val varsletBeløp: Long,
) : Brev {
    companion object {
        fun opprett(
            varsletBeløp: Long,
        ): Brev {
            return Varselbrev(
                id = UUID.randomUUID(),
                varsletBeløp = varsletBeløp,
                opprettetDato = LocalDate.now(),
            )
        }
    }

    override fun tilEntity(): BrevEntity {
        return BrevEntity(
            brevType = Brevtype.VARSEL_BREV,
            internId = id,
            opprettetDato = opprettetDato,
            varsletBeløp = varsletBeløp,
        )
    }
}
