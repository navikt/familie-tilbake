package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Totrinnsresultatsgrunnlag(@Id
                                     val id: UUID = UUID.randomUUID(),
                                     val behandlingId: UUID,
                                     val grupperingFaktaFeilutbetalingId: UUID?,
                                     val grupperingVurdertForeldelseId: UUID,
                                     @Column("vilkar_id")
                                     val vilkårId: UUID,
                                     val aktiv: Boolean = true,
                                     val versjon: Int = 0,
                                     @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                     val sporbar: Sporbar = Sporbar())