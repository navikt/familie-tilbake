package no.nav.familie.tilbake.totrinn.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnsresultatsgrunnlag(@Id
                                     val id: UUID = UUID.randomUUID(),
                                     val behandlingId: UUID,
                                     val faktaFeilutbetalingId: UUID,
                                     val vurdertForeldelseId: UUID? = null,
                                     @Column("vilkarsvurdering_id")
                                     val vilkårsvurderingId: UUID? = null,
                                     val aktiv: Boolean = true,
                                     @Version
                                     val versjon: Long = 0,
                                     @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                     val sporbar: Sporbar = Sporbar())
