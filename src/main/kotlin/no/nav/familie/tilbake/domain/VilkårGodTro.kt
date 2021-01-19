package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkar_god_tro")
data class VilkårGodTro(@Id
                        val id: UUID = UUID.randomUUID(),
                        @Column("vilkarsperiode_Id")
                        val vilkårsperiodeId: UUID,
                        @Column("belop_er_i_behold")
                        val beløpErIBehold: Boolean,
                        @Column("belop_tilbakekreves")
                        val beløpTilbakekreves: Long?,
                        val begrunnelse: String,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())