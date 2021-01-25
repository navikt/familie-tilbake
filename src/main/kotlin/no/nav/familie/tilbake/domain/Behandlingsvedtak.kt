package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Behandlingsvedtak(@Id
                             val id: UUID = UUID.randomUUID(),
                             val vedtaksdato: LocalDate,
                             val ansvarligSaksbehandler: String,
                             val versjon: Int = 0,
                             val iverksettingsstatus: Iverksettingsstatus = Iverksettingsstatus.IKKE_IVERKSATT,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())

enum class Iverksettingsstatus {
    IKKE_IVERKSATT,
    UNDER_IVERKSETTING,
    IVERKSATT,
    UDEFINERT
}
