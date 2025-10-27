package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.BrevmottakerSteg
import java.util.UUID

data class BrevmottakerStegEntity(
    val id: UUID = UUID.randomUUID(),
    val behandlingRef: UUID? = null,
    val aktivert: Boolean,
    val defaultMottakerEntity: RegistrertBrevmottakerEntity,
    val registrertBrevmottakerEntity: RegistrertBrevmottakerEntity,
) {
    fun fraEntity(): BrevmottakerSteg {
        val brevmottaker = BrevmottakerSteg(
            id = id,
            aktivert = aktivert,
            defaultMottaker = defaultMottakerEntity.fraEntity(),
        )
        brevmottaker.registrertBrevmottaker = registrertBrevmottakerEntity.fraEntity()
        return brevmottaker
    }
}
