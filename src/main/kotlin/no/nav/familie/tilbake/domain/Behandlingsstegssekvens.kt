package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsstegssekvens(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   val behandlingsstegstypeId: UUID,
                                   val behandlingstype: Behandlingstype,
                                   val sekvensnummer: Int,
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())