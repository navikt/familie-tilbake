package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnsresultatsgrunnlag(@Id
                                     val id: UUID = UUID.randomUUID(),
                                     val behandlingId: UUID,
                                     val faktaFeilutbetalingId: UUID?,
                                     val grupperingVurdertForeldelseId: UUID,
                                     @Column("vilkarsvurdering_id")
                                     val vilkårsvurderingId: UUID,
                                     val aktiv: Boolean = true,
                                     val versjon: Int = 0,
                                     @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                     val sporbar: Sporbar = Sporbar())
