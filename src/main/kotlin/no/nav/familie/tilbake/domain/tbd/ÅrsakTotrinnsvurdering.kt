package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("arsak_totrinnsvurdering")
data class ÅrsakTotrinnsvurdering(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  val totrinnsvurderingId: UUID,
                                  @Column("arsakstype")
                                  val årsakstype: Årsakstype,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())