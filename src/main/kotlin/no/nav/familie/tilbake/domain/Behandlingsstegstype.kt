package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsstegstype(@Id
                                val id: UUID = UUID.randomUUID(),
                                val kode: String,
                                val navn: String,
                                val definertBehandlingsstatus: Behandlingsstatus,
                                val beskrivelse: String?,
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())