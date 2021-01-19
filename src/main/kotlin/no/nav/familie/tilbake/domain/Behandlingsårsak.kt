package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("behandlingsarsak")
data class Behandlingsårsak(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val originalBehandlingId: UUID?,
                            val type: String,
                            val versjon: Int = 0,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())