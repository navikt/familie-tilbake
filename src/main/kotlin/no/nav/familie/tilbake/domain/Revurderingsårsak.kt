package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("Revurderingsarsak")
data class Revurderingsårsak(@Id
                             val id: UUID = UUID.randomUUID(),
                             val aksjonspunktId: UUID,
                             @Column("arsakstype")
                             val årsakstype: String,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())