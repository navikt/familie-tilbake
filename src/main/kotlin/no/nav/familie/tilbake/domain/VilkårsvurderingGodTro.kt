package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkarsvurdering_god_tro")
data class VilkårsvurderingGodTro(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  @Column("vilkarsvurderingsperiode_Id")
                                  val vilkårsperiodeId: UUID,
                                  @Column("belop_er_i_behold")
                                  val beløpErIBehold: Boolean,
                                  @Column("belop_tilbakekreves")
                                  val beløpTilbakekreves: Long?,
                                  val begrunnelse: String,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())