package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsresultat(@Id
                               val id: UUID = UUID.randomUUID(),
                               val behandlingId: UUID,
                               val versjon: Int = 0,
                               val type: String = "IKKE_FASTSATT",
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())