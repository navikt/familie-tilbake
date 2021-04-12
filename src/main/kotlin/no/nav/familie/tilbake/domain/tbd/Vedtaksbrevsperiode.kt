package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vedtaksbrevsperiode(@Id
                               val id: UUID = UUID.randomUUID(),
                               val behandlingId: UUID,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val periode: Periode,
                               val fritekst: String,
                               val fritekststype: Fritekstavsnittstype,
                               @Version
                               val versjon: Long = 0,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())

enum class Fritekstavsnittstype {
    FAKTA,
    FORELDELSE,
    VILKÅR,
    SÆRLIGE_GRUNNER,
    SÆRLIGE_GRUNNER_ANNET
}
