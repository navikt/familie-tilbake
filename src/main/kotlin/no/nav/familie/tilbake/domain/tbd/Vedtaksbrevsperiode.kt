package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Vedtaksbrevsperiode(@Id
                               val id: UUID = UUID.randomUUID(),
                               val behandlingId: UUID,
                               val fom: LocalDate,
                               val tom: LocalDate,
                               val fritekst: String,
                               val fritekststype: Friteksttype,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())

enum class Friteksttype {
    FAKTA_AVSNITT,
    FORELDELSE_AVSNITT,
    VILKÅR_AVSNITT,
    SÆRLIGE_GRUNNER_AVSNITT,
    SÆRLIGE_GRUNNER_ANNET_AVSNITT
}
