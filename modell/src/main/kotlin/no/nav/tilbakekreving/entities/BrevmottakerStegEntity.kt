package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.BrevmottakerSteg

data class BrevmottakerStegEntity(
    val aktivert: Boolean,
    val defaultMottakerEntity: RegistrertBrevmottakerEntity,
    val registrertBrevmottakerEntity: RegistrertBrevmottakerEntity,
) {
    fun fraEntity(): BrevmottakerSteg {
        val brevmottaker = BrevmottakerSteg(
            aktivert,
            defaultMottakerEntity.fraEntity(),
        )
        brevmottaker.registrertBrevmottaker = registrertBrevmottakerEntity.fraEntity()
        return brevmottaker
    }
}
