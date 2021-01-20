package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkarsvurdering")
data class Vilkårsvurdering(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val aktiv: Boolean = true,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())